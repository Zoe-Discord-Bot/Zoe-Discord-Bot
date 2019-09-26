package ch.kalunight.zoe.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class InfoCardsWorker implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(InfoCardsWorker.class);

  private Server server;

  private TextChannel controlPanel;

  private LeagueAccount account;

  private CurrentGameInfo currentGameInfo;

  public InfoCardsWorker(Server server, TextChannel controlPanel, LeagueAccount account, CurrentGameInfo currentGameInfo) {
    this.server = server;
    this.controlPanel = controlPanel;
    this.account = account;
    this.currentGameInfo = currentGameInfo;
  }


  @Override
  public void run() {
    try {
      
      logger.debug("InfoCards worker lauched for the game id: {}", currentGameInfo.getGameId());
      if(controlPanel.canTalk()) {
        generateInfoCard(controlPanel, account, currentGameInfo);
        
        generatePlayersComments();
        
        
        RiotApiUsageChannelRefresh.incrementInfocardCount();
        logger.debug("InfoCards worker has ended correctly for the game id: {}", currentGameInfo.getGameId());
      }else {
        logger.info("Impossible to send message in the infochannel of the guild id: {}", server.getGuild().getId());
      }
      
    }catch(InsufficientPermissionException e) {
      logger.info("Missing permissions with the generation of infocards in the guild id {} : {}",
          server.getGuild().getId(), e.getPermission());
    }catch(Exception e) {
      logger.warn("A unexpected exception has been catched ! Error message : {}", e.getMessage(), e);
    }finally {
      server.setLastRefresh(DateTime.now());
    }
  }

  private void generatePlayersComments() {
    
    
    
    
    
  }


  private void generateInfoCard(TextChannel controlPanel, LeagueAccount account, CurrentGameInfo currentGameInfo) {

    List<Player> listOfPlayerInTheGame = InfoPanelRefresherUtil.checkIfOthersPlayersIsKnowInTheMatch(currentGameInfo, server);
    Player player = server.getPlayerByLeagueAccount(account);
    InfoCard card = null;

    if(listOfPlayerInTheGame.size() == 1) {
      MessageEmbed messageCard = MessageBuilderRequest.createInfoCard1summoner(player.getDiscordUser(), account.getSummoner(),
          currentGameInfo, account.getRegion());
      if(messageCard != null) {
        card = new InfoCard(listOfPlayerInTheGame, messageCard, currentGameInfo);
      }
    } else if(listOfPlayerInTheGame.size() > 1) {
      MessageEmbed messageCard =
          MessageBuilderRequest.createInfoCardsMultipleSummoner(listOfPlayerInTheGame, currentGameInfo, account.getRegion());

      if(messageCard != null) {
        card = new InfoCard(listOfPlayerInTheGame, messageCard, currentGameInfo);
      }
    }

    if(card != null) {
      List<Player> players = card.getPlayers();

      StringBuilder title = new StringBuilder();
      generateInfoCardTitle(card, players, title);

      card.setTitle(controlPanel.sendMessage(title.toString()).complete());
      card.setMessage(controlPanel.sendMessage(card.getCard()).complete());

      server.getControlePannel().getInfoCards().add(card);
    }
  }

  private void generateInfoCardTitle(InfoCard card, List<Player> players, StringBuilder title) {
    title.append("Info on the game of");

    List<String> playersName = NameConversion.getListNameOfPlayers(players);

    Set<Player> cardPlayersNotTwice = new HashSet<>();
    for(Player playerToCheck : card.getPlayers()) {
      cardPlayersNotTwice.add(playerToCheck);
    }

    for(int j = 0; j < cardPlayersNotTwice.size(); j++) {
      if(j == 0) {
        title.append(" " + playersName.get(j));
      } else if(j + 1 == playersName.size()) {
        title.append(" and of " + playersName.get(j));
      } else if(j + 2 == playersName.size()) {
        title.append(" " + playersName.get(j));
      } else {
        title.append(" " + playersName.get(j) + ",");
      }
    }
  }

}
