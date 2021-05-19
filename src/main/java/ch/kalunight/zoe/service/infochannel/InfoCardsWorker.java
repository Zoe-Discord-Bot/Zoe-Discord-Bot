package ch.kalunight.zoe.service.infochannel;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Stopwatch;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.GameInfoCardStatus;
import ch.kalunight.zoe.repositories.GameInfoCardRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.service.RiotApiUsageChannelRefresh;
import ch.kalunight.zoe.service.ServerChecker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import ch.kalunight.zoe.util.MessageBuilderRequestUtil;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class InfoCardsWorker implements Runnable {

  public static final int GAME_LENGTH_AFTER_WE_NOT_GENERATE_IN_SEC = 420;
  
  private static final Logger logger = LoggerFactory.getLogger(InfoCardsWorker.class);
  
  private DTO.Server server;

  private TextChannel controlPanel;

  private DTO.LeagueAccount account;

  private DTO.CurrentGameInfo currentGameInfo;
  
  private DTO.GameInfoCard gameInfoCard;
  
  private boolean forceRefreshCache;

  public InfoCardsWorker(DTO.Server server, TextChannel controlPanel, DTO.LeagueAccount account, DTO.CurrentGameInfo currentGameInfo,
      DTO.GameInfoCard gameInfoCard, boolean forceRefreshCache) {
    this.server = server;
    this.controlPanel = controlPanel;
    this.account = account;
    this.currentGameInfo = currentGameInfo;
    this.gameInfoCard = gameInfoCard;
    this.forceRefreshCache = forceRefreshCache;
  }

  @Override
  public void run() {
    try {
      if(controlPanel.canTalk()) {
        logger.info("Start generate infocards for the account {} ({})", account.getSummoner(forceRefreshCache).getName(),  account.leagueAccount_server.getName());

        Stopwatch stopWatch = Stopwatch.createStarted();
        
        if(theGameHaveToBeGenerate()) {
          generateInfoCard(account, currentGameInfo, controlPanel.getJDA());
          stopWatch.stop();
          logger.info("Infocards generation done in {} secs.", stopWatch.elapsed(TimeUnit.SECONDS));

          RiotApiUsageChannelRefresh.incrementInfocardCount();
        }else {
          sendOverloadedMessage();
          stopWatch.stop();
          logger.info("Infocards canceled in {} secs.", stopWatch.elapsed(TimeUnit.SECONDS));
          
          RiotApiUsageChannelRefresh.incrementInfocardCancelCount();
        }

        logger.debug("InfoCards worker has ended correctly for the game id: {}", currentGameInfo.currentgame_currentgame.getGameId());
      }else {
        logger.info("Impossible to send message in the infochannel of the guild id: {}", server.serv_guildId);
      }

    }catch(InsufficientPermissionException e) {
      logger.info("Missing permissions with the generation of infocards in the guild id {} : {}",
          server.serv_guildId, e.getPermission());
    }catch(Exception e) {
      logger.warn("A unexpected exception has been catched ! Error message : {}", e.getMessage(), e);
    }finally {
      try {
        GameInfoCardRepository.updateGameInfoCardStatusWithId(gameInfoCard.gamecard_id, GameInfoCardStatus.IN_WAIT_OF_MATCH_ENDING);
        ServerRepository.updateTimeStamp(server.serv_guildId, LocalDateTime.now());
      } catch(SQLException e) {
        logger.error("SQL error when updating the timestamp of the server !", e);
      }
      ServerChecker.getServerRefreshService().taskEnded(null);
    }
  }

  private void sendOverloadedMessage() throws SQLException {
    
    List<DTO.Player> listOfPlayerInTheGame = InfoPanelRefresherUtil.checkIfOthersPlayersIsKnowInTheMatch(currentGameInfo, server);
    
    ArrayList<DTO.Player> playersNotTwice = new ArrayList<>();

    for(DTO.Player player : listOfPlayerInTheGame) {
      if(!playersNotTwice.contains(player)) {
        playersNotTwice.add(player);
      }
    }
    
    StringBuilder baseString = new StringBuilder();
    MessageBuilderRequestUtil.getReadableListOfPlayers(baseString, controlPanel.getJDA(), playersNotTwice, LanguageManager.getText(server.getLanguage(), "infoCardsGameInfoAndOf"));
    
    String errorMessage = String.format(LanguageManager.getText(server.getLanguage(), "couldNotSendInfoCardsOverloaded"), baseString.toString());
    
    Message message = controlPanel.sendMessage(errorMessage).complete();

    GameInfoCardRepository.updateGameInfoCardsMessagesWithId(-1, message.getIdLong(),
        LocalDateTime.now(), gameInfoCard.gamecard_id);
  }

  private boolean theGameHaveToBeGenerate() {
    try {
      CurrentGameInfo currentGameRefreshed = Zoe.getRiotApi().getActiveGameBySummonerWithRateLimit(account.leagueAccount_server, account.leagueAccount_summonerId);

      if(currentGameRefreshed != null && currentGameRefreshed.getGameId() == currentGameInfo.currentgame_gameid 
          && currentGameRefreshed.getGameLength() < GAME_LENGTH_AFTER_WE_NOT_GENERATE_IN_SEC) {
        return true;
      }else {
        return false;
      }
    }catch(RiotApiException e) {
      logger.warn("A riot exception has been throw! Game not generated.", e);
      return false;
    }
  }

  private void generateInfoCard(DTO.LeagueAccount account, DTO.CurrentGameInfo currentGameInfo, JDA jda)
      throws SQLException {

    List<DTO.Player> listOfPlayerInTheGame = InfoPanelRefresherUtil.checkIfOthersPlayersIsKnowInTheMatch(currentGameInfo, server);

    InfoCard card = null;

    if(!listOfPlayerInTheGame.isEmpty()) {
      MessageEmbed messageCard =
          MessageBuilderRequest.createInfoCard(listOfPlayerInTheGame, currentGameInfo.currentgame_currentgame,
              account.leagueAccount_server, server, jda);

      if(messageCard != null) {
        card = new InfoCard(listOfPlayerInTheGame, messageCard, currentGameInfo.currentgame_currentgame);
      }
    }

    if(card != null) {
      List<DTO.Player> players = card.getPlayers();

      StringBuilder title = new StringBuilder();
      MessageBuilderRequestUtil.createTitle(players, currentGameInfo.currentgame_currentgame, title, server.getLanguage(), false, jda);

      DTO.InfoChannel infochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);
      TextChannel infoChannel = jda.getGuildById(server.serv_guildId).getTextChannelById(infochannel.infochannel_channelid);

      DTO.GameInfoCard gameCard = GameInfoCardRepository.getGameInfoCardsWithCurrentGameId(server.serv_guildId, currentGameInfo.currentgame_id);
      if(infoChannel != null) {
        Message message = infoChannel.sendMessage(title.toString()).embed(card.getCard()).complete();

        GameInfoCardRepository.updateGameInfoCardsMessagesWithId(-1, message.getIdLong(),
            LocalDateTime.now(), gameCard.gamecard_id);
      }
    }
  }
}
