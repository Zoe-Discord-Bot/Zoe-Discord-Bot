package ch.kalunight.zoe.service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Stopwatch;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import ch.kalunight.zoe.util.MessageBuilderRequestUtil;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class InfoCardsWorker implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(InfoCardsWorker.class);

  private DTO.Server server;

  private TextChannel controlPanel;

  private DTO.LeagueAccount account;

  private DTO.CurrentGameInfo currentGameInfo;

  public InfoCardsWorker(DTO.Server server, TextChannel controlPanel, DTO.LeagueAccount account, DTO.CurrentGameInfo currentGameInfo) {
    this.server = server;
    this.controlPanel = controlPanel;
    this.account = account;
    this.currentGameInfo = currentGameInfo;
  }

  @Override
  public void run() {
    try {
      if(controlPanel.canTalk()) {
        logger.info("Start generate infocards for the account " + account.getSummoner().getName() 
            + " (" + account.getRegion().getName() + ")");
        
        Stopwatch stopWatch = Stopwatch.createStarted();
        generateInfoCard(controlPanel, account, currentGameInfo);
        stopWatch.stop();
        logger.info("Infocards generation done in {} secs.", stopWatch.elapsed(TimeUnit.SECONDS));
        
        RiotApiUsageChannelRefresh.incrementInfocardCount();
        logger.debug("InfoCards worker has ended correctly for the game id: {}", currentGameInfo.getGameId());
      }else {
        logger.info("Impossible to send message in the infochannel of the guild id: {}", server.getGuildId());
      }
      
    }catch(InsufficientPermissionException e) {
      logger.info("Missing permissions with the generation of infocards in the guild id {} : {}",
          server.getGuildId(), e.getPermission());
    }catch(Exception e) {
      logger.warn("A unexpected exception has been catched ! Error message : {}", e.getMessage(), e);
    }finally {
      server.setLastRefresh(DateTime.now());
    }
  }

  private void generateInfoCard(TextChannel controlPanel, LeagueAccount account, CurrentGameInfo currentGameInfo) {

    List<Player> listOfPlayerInTheGame = InfoPanelRefresherUtil.checkIfOthersPlayersIsKnowInTheMatch(currentGameInfo, server);
    Player player = server.getPlayerByLeagueAccount(account);
    InfoCard card = null;

    if(listOfPlayerInTheGame.size() == 1) {
      MessageEmbed messageCard = MessageBuilderRequest.createInfoCard1summoner(player.getDiscordUser(), account.getSummoner(),
          currentGameInfo, account.getRegion(), server.getLangage());
      if(messageCard != null) {
        card = new InfoCard(listOfPlayerInTheGame, messageCard, currentGameInfo);
      }
    } else if(listOfPlayerInTheGame.size() > 1) {
      MessageEmbed messageCard =
          MessageBuilderRequest.createInfoCardsMultipleSummoner(listOfPlayerInTheGame, currentGameInfo, account.getRegion(), server.getLangage());

      if(messageCard != null) {
        card = new InfoCard(listOfPlayerInTheGame, messageCard, currentGameInfo);
      }
    }

    if(card != null) {
      List<Player> players = card.getPlayers();

      StringBuilder title = new StringBuilder();
      MessageBuilderRequestUtil.createTitle(players, currentGameInfo, title, server.getLangage(), false);

      card.setTitle(controlPanel.sendMessage(title.toString()).complete());
      card.setMessage(controlPanel.sendMessage(card.getCard()).complete());

      server.getControlePannel().getInfoCards().add(card);
    }
  }

}
