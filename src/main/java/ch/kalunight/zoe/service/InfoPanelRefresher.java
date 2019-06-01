package ch.kalunight.zoe.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.LeagueAccount;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.Team;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.requests.ErrorResponse;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.constant.CallPriority;

public class InfoPanelRefresher implements Runnable {

  private static final int INFO_CARDS_MINUTES_LIVE_TIME = 30;

  private static final Logger logger = LoggerFactory.getLogger(InfoPanelRefresher.class);

  private Server server;

  public InfoPanelRefresher(Server server) {
    this.server = server;
  }

  @Override
  public void run() {
    ServerData.getServersIsInTreatment().put(server.getGuild().getId(), true);
    try {

      cleanRegisteredPlayerNoLongerInGuild();

      if(server.getInfoChannel() != null) {
        if(server.getControlePannel().getInfoPanel().isEmpty()) {
          server.getControlePannel().getInfoPanel()
          .add(server.getInfoChannel().sendMessage("__**Control Panel**__\n \n*Loading...*").complete());
        }

        ArrayList<String> infoPanels = CommandEvent.splitMessage(refreshPannel());

        if(server.getInfoChannel() != null || server.getGuild().getTextChannelById(server.getInfoChannel().getId()) != null) {

          if(infoPanels.size() < server.getControlePannel().getInfoPanel().size()) {
            int nbrMessageToDelete = server.getControlePannel().getInfoPanel().size() - infoPanels.size();
            for(int i = 0; i < nbrMessageToDelete; i++) {
              Message message = server.getControlePannel().getInfoPanel().get(i);
              message.delete().queue();
              server.getControlePannel().getInfoPanel().remove(message);
            }

          } else {
            int nbrMessageToAdd = infoPanels.size() - server.getControlePannel().getInfoPanel().size();
            for(int i = 0; i < nbrMessageToAdd; i++) {
              server.getControlePannel().getInfoPanel().add(server.getInfoChannel().sendMessage("loading ...").complete());
            }
          }

          for(int i = 0; i < infoPanels.size(); i++) {
            server.getControlePannel().getInfoPanel().get(i).editMessage(infoPanels.get(i)).queue();
          }

          manageInfoCards();

          cleaningInfoChannel();
        } else {
          server.setInfoChannel(null);
          server.setControlePannel(new ControlPannel());
        }
      }
    } catch(NullPointerException e) {
      logger.info("The Thread has crashed normally because of deletion of infoChannel :", e);
    } catch(Exception e) {
      logger.warn("The thread got a unexpected error :", e);
    } finally {
      ServerData.getServersIsInTreatment().put(server.getGuild().getId(), false);
    }
  }


  private void cleanRegisteredPlayerNoLongerInGuild() {

    Iterator<Player> iter = server.getPlayers().iterator();

    while (iter.hasNext()) {
      Player player = iter.next();
      if(player.getDiscordUser() == null || server.getGuild().getMemberById(player.getDiscordUser().getId()) == null) {
        iter.remove();
        for(Team team : server.getTeams()) {
          team.getPlayers().remove(player);
        }
      }
    }
  }

  private void cleaningInfoChannel() {

    List<Message> messagesToCheck = server.getInfoChannel().getIterableHistory().stream()
        .limit(1000)
        .filter(m-> m.getAuthor().getId().equals(Zoe.getJda().getSelfUser().getId()))
        .collect(Collectors.toList());

    List<Message> messagesToDelete = new ArrayList<>();

    for(Message messageToCheck : messagesToCheck) {
      if(!messageToCheck.getCreationTime().isBefore(OffsetDateTime.now().minusHours(1)) || server.getControlePannel().getInfoPanel().contains(messageToCheck)) {
        continue;
      }

      messagesToDelete.add(messageToCheck);
    }

    if(messagesToDelete.isEmpty()) {
      return;
    }

    if(messagesToDelete.size() > 1) {
      server.getInfoChannel().purgeMessages(messagesToDelete);
    }else {
      for(Message messageToDelete : messagesToDelete) {
        messageToDelete.delete().queue();
      }
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
        try {
          cardsToRemove.get(i).getMessage().delete().complete();
        } catch(ErrorResponseException e) {
          if(e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
            logger.info("Message already deleted : {}", e.getMessage());
          } else {
            logger.warn("Unhandle error : {}", e.getMessage());
            throw e;
          }
        }

        try {
          cardsToRemove.get(i).getTitle().delete().complete();
        } catch(ErrorResponseException e) {
          if(e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
            logger.info("Message already deleted : {}", e.getMessage());
          } else {
            logger.warn("Unhandle error : {}", e.getMessage());
            throw e;
          }
        }
        server.getControlePannel().getInfoCards().remove(cardsToRemove.get(i));
      } catch(Exception e) {
        logger.warn("Impossible to delete message, retry next time : {}", e.getMessage(), e);
      }
    }
  }

  private List<InfoCard> createInfoCards(TextChannel controlPannel) {

    ArrayList<InfoCard> cards = new ArrayList<>();
    ArrayList<LeagueAccount> accountAlreadyGenerated = new ArrayList<>();

    for(LeagueAccount account : server.getAllAccountsOfTheServer()) {

      if(!accountAlreadyGenerated.contains(account)) {

        CurrentGameInfo currentGameInfo = account.getCurrentGameInfo();

        if(currentGameInfo != null && !server.getCurrentGamesIdAlreadySended().contains(currentGameInfo.getGameId())) {
          List<Player> listOfPlayerInTheGame = checkIfOthersPlayersIsKnowInTheMatch(currentGameInfo);
          Player player = server.getPlayerByLeagueAccount(account);
          InfoCard card = null;

          if(listOfPlayerInTheGame.size() == 1) {
            MessageEmbed messageCard = MessageBuilderRequest.createInfoCard1summoner(player.getDiscordUser(), account.getSummoner(),
                currentGameInfo, account.getRegion());
            if(messageCard != null) {
              card = new InfoCard(listOfPlayerInTheGame, messageCard);
            }
          } else if(listOfPlayerInTheGame.size() > 1) {
            MessageEmbed messageCard =
                MessageBuilderRequest.createInfoCardsMultipleSummoner(listOfPlayerInTheGame, currentGameInfo, account.getRegion());

            if(messageCard != null) {
              card = new InfoCard(listOfPlayerInTheGame, messageCard);
            }
          }

          accountAlreadyGenerated.addAll(checkIfOthersPlayersInKnowInTheMatch(currentGameInfo));
          server.getCurrentGamesIdAlreadySended().add(currentGameInfo.getGameId());

          if(card != null) {
            List<Player> players = card.getPlayers();

            StringBuilder title = new StringBuilder();
            title.append("Info on the game of");

            List<String> playersName = NameConversion.getListNameOfPlayers(players);

            for(int j = 0; j < card.getPlayers().size(); j++) {
              if(playersName.size() == 1) {
                title.append(" " + playersName.get(j));
              } else if(j + 1 == playersName.size()) {
                title.append(" and of " + playersName.get(j));
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

    for(Player player : server.getPlayers()) {
      for(LeagueAccount leagueAccount : player.getLolAccounts()) {
        for(CurrentGameParticipant participant : currentGameInfo.getParticipants()) {
          if(participant.getSummonerId().equals(leagueAccount.getSummoner().getId()) && !listOfPlayers.contains(player)) {
            listOfPlayers.add(player);
          }
        }
      }
    }
    return listOfPlayers;
  }
  
  private List<LeagueAccount> checkIfOthersPlayersInKnowInTheMatch(CurrentGameInfo currentGameInfo){
    
    ArrayList<LeagueAccount> listOfAccounts = new ArrayList<>();
    
    for(Player player : server.getPlayers()) {
      for(LeagueAccount leagueAccount : player.getLolAccounts()) {
        for(CurrentGameParticipant participant : currentGameInfo.getParticipants()) {
          if(participant.getSummonerId().equals(leagueAccount.getSummoner().getId()) && !listOfAccounts.contains(leagueAccount)) {
            listOfAccounts.add(leagueAccount);
          }
        }
      }
    }
    return listOfAccounts;
  }

  private String refreshPannel() {

    final List<Team> teamList = server.getAllPlayerTeams();
    final StringBuilder stringMessage = new StringBuilder();

    for(Player player : server.getPlayers()) {
      player.refreshAllLeagueAccounts(CallPriority.NORMAL);
    }

    stringMessage.append("__**Control Panel**__\n \n");

    for(Team team : teamList) {

      stringMessage.append("**" + team.getName() + "**\n \n");

      List<Player> playersList = team.getPlayers();

      for(Player player : playersList) {

        List<LeagueAccount> leagueAccounts = player.getLeagueAccountsInGame();

        if(leagueAccounts.isEmpty()) {
          stringMessage.append(player.getDiscordUser().getAsMention() + " : Not in game\n");
        }else if (leagueAccounts.size() == 1) {
          stringMessage.append(player.getDiscordUser().getAsMention() + " : " 
              + InfoPanelRefresherUtil.getCurrentGameInfoStringForOneAccount(leagueAccounts.get(0)) + "\n");
        }else {
          stringMessage.append(player.getDiscordUser().getAsMention() + " : Multiples accounts are in game\n"
              + InfoPanelRefresherUtil.getCurrentGameInfoStringForMultipleAccounts(leagueAccounts));
        }
      }
      stringMessage.append(" \n");
    }

    stringMessage.append("*Refreshed every 3 minutes*");

    return stringMessage.toString();
  }


}
