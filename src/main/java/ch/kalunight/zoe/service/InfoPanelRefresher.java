package ch.kalunight.zoe.service;

import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.Team;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class InfoPanelRefresher implements Runnable {

  private static final int INFO_CARDS_MINUTES_LIVE_TIME = 30;

  private static final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("HH:mm");
  
  private static final Logger logger = LoggerFactory.getLogger(InfoPanelRefresher.class);

  private Server server;

  public InfoPanelRefresher(Server server) {
    this.server = server;
  }

  @Override
  public void run() {
    if(server.getInfoChannel() != null) {
      if(server.getControlePannel().getInfoPanel().isEmpty()) {
        server.getControlePannel().getInfoPanel().add(server.getInfoChannel().sendMessage("__**Control Panel**__\n \n*Loading...*").complete());
      }

      ArrayList<String> infoPanels = CommandEvent.splitMessage(refreshPannel());
      
      if(infoPanels.size() < server.getControlePannel().getInfoPanel().size()) {
        int nbrMessageToDelete = server.getControlePannel().getInfoPanel().size() - infoPanels.size();
        for(int i = 0; i < nbrMessageToDelete; i++) {
          Message message = server.getControlePannel().getInfoPanel().get(i);
          message.delete().queue();
          server.getControlePannel().getInfoPanel().remove(message);
        }
        
      }else {
        int nbrMessageToAdd = infoPanels.size() - server.getControlePannel().getInfoPanel().size();
        for(int i = 0; i < nbrMessageToAdd; i++) {
          server.getControlePannel().getInfoPanel().add(server.getInfoChannel().sendMessage("loading ...").complete());
        }
      }
      
      for(int i = 0; i < infoPanels.size(); i++) {
        server.getControlePannel().getInfoPanel().get(i).editMessage(infoPanels.get(i)).queue();
      }

      manageInfoCards();
    }
  }
  
  
  private void manageInfoCards() {
    TextChannel controlPannel = server.getInfoChannel();

    List<InfoCard> messageSended = createInfoCards(controlPannel);

    server.getControlePannel().getInfoCards().addAll(messageSended);

    deleteOlderInfoCards();
  }

  private void deleteOlderInfoCards() {
    List<InfoCard> cardsToRemove = new ArrayList<>();

    for(int i = 0; i < server.getControlePannel().getInfoCards().size(); i++) {
      InfoCard card = server.getControlePannel().getInfoCards().get(i);

      if(card.getCreationTime().plusMinutes(INFO_CARDS_MINUTES_LIVE_TIME).isBeforeNow()) {
        cardsToRemove.add(card);
      }
    }

    for(int i = 0; i < cardsToRemove.size(); i++) {
      try {
        cardsToRemove.get(i).getMessage().delete().complete();
        cardsToRemove.get(i).getTitle().delete().complete();
        server.getControlePannel().getInfoCards().remove(cardsToRemove.get(i));
      } catch(Exception e) {
        logger.warn("Impossible de delete message : {}", e.getMessage(), e);
      }
    }
  }

  private List<InfoCard> createInfoCards(TextChannel controlPannel) {

    ArrayList<InfoCard> cards = new ArrayList<>();
    ArrayList<Player> playersAlreadyGenerated = new ArrayList<>();

    for(int i = 0; i < server.getPlayers().size(); i++) {
      Player player = server.getPlayers().get(i);

      if(!playersAlreadyGenerated.contains(player)) {

        CurrentGameInfo currentGameInfo = server.getCurrentGames().get(player.getSummoner().getId());

        if(currentGameInfo != null && !server.getCurrentGamesIdAlreadySended().contains(currentGameInfo.getGameId())) {
          List<Player> listOfPlayerInTheGame = checkIfOthersPlayersIsKnowInTheMatch(currentGameInfo);
          InfoCard card = null;

          if(listOfPlayerInTheGame.size() == 1) {
            MessageEmbed messageCard =
                MessageBuilderRequest.createInfoCard1summoner(player.getDiscordUser(), player.getSummoner(), currentGameInfo, player.getRegion());
            if(messageCard != null) {
              card = new InfoCard(listOfPlayerInTheGame, messageCard);
            }
          } else if(listOfPlayerInTheGame.size() > 1) {
            MessageEmbed messageCard = MessageBuilderRequest.createInfoCardsMultipleSummoner(listOfPlayerInTheGame, currentGameInfo, player.getRegion());

            if(messageCard != null) {
              card = new InfoCard(listOfPlayerInTheGame, messageCard);
            }
          }

          playersAlreadyGenerated.addAll(listOfPlayerInTheGame);
          server.getCurrentGamesIdAlreadySended().add(currentGameInfo.getGameId());

          if(card != null) {
            List<Player> players = card.getPlayers();

            StringBuilder title = new StringBuilder();
            title.append("Info sur la partie de");

            List<String> playersName = NameConversion.getListNameOfPlayers(players);

            for(int j = 0; j < card.getPlayers().size(); j++) {
              if(playersName.size() == 1) {
                title.append(" " + playersName.get(j));
              } else if(j + 1 == playersName.size()) {
                title.append(" et de " + playersName.get(j));
              } else if(j + 2 == playersName.size()) {
                title.append(" " + playersName.get(j));
              } else {
                title.append(" " + playersName.get(j) + ",");
              }
            }

            card.setTitle(controlPannel.sendMessage(title.toString()).complete());
            card.setMessage(controlPannel.sendMessage(card.getCard()).complete());

            cards.add(card);
          }
        }
      }
    }
    
    server.clearOldMatchOfSendedGamesIdList();
    return cards;
  }

  private List<Player> checkIfOthersPlayersIsKnowInTheMatch(CurrentGameInfo currentGameInfo) {

    ArrayList<Player> listOfPlayers = new ArrayList<>();

    for(int i = 0; i < server.getPlayers().size(); i++) {
      for(int j = 0; j < currentGameInfo.getParticipants().size(); j++) {
        if(currentGameInfo.getParticipants().get(j).getSummonerId().equals(server.getPlayers().get(i).getSummoner().getId())) {
          listOfPlayers.add(server.getPlayers().get(i));
        }
      }
    }
    return listOfPlayers;
  }

  private String refreshPannel() {

    final List<Team> teamList = server.getAllPlayerTeams();
    final StringBuilder stringMessage = new StringBuilder();

    for(Player player : server.getPlayers()) {
      CurrentGameInfo actualGame = null;
      try {
        actualGame = Zoe.getRiotApi().getActiveGameBySummoner(player.getRegion(), player.getSummoner().getId());
      } catch(RiotApiException e) {
        logger.info(e.getMessage());
      }
      server.getCurrentGames().put(player.getSummoner().getId(), actualGame); // Can be null
    }

    stringMessage.append("__**Control Panel**__\n \n");

    for(Team team : teamList) {

      stringMessage.append("**" + team.getName() + "**\n \n");

      List<Player> playersList = team.getPlayers();

      for(Player player : playersList) {
        stringMessage
            .append(player.getSummoner().getName() + " (" + player.getDiscordUser().getAsMention() + ") : ");

        CurrentGameInfo actualGame = server.getCurrentGames().get(player.getSummoner().getId());

        if(actualGame == null) {
          stringMessage.append("Not in game\n");
        } else {
          stringMessage.append(RiotRequest.getActualGameStatus(actualGame) + "\n");
        }

      }
      stringMessage.append(" \n");
    }

    stringMessage.append(
        "\nThe last refreshment was at " + DateTime.now().plusHours(1).toString(timeFormatter) + " | *Refreshed every 3 minutes*");

    return stringMessage.toString();
  }


}
