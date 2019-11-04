package ch.kalunight.zoe.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.model.player_data.Team;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.constant.CallPriority;

public class InfoPanelRefresher implements Runnable {

  private static final int INFO_CARDS_MINUTES_LIVE_TIME = 20;

  private static final Logger logger = LoggerFactory.getLogger(InfoPanelRefresher.class);

  private Server server;

  private boolean needToWait = false;

  public InfoPanelRefresher(Server server) {
    this.server = server;
  }

  public InfoPanelRefresher(Server server, boolean needToWait) {
    this.server = server;
    this.needToWait = needToWait;
  }


  @Override
  public void run() {
    try {

      if(server.getInfoChannel() != null) {

        if(needToWait) {
          TimeUnit.SECONDS.sleep(3);
        }

        for(Player player : server.getPlayers()) {
          player.refreshAllLeagueAccounts(CallPriority.NORMAL);
        }

        cleanRegisteredPlayerNoLongerInGuild();
        server.clearOldMatchOfSendedGamesIdList();
        deleteOlderInfoCards();

        ArrayList<String> infoPanels = CommandEvent.splitMessage(refreshPannel());

        if(server.getInfoChannel() != null && server.getGuild().getTextChannelById(server.getInfoChannel().getId()) != null) {

          cleanInfoChannelCache();

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

          if(server.getConfig().getInfoCardsOption().isOptionActivated()) {
            manageInfoCards();
          }

          try {
            cleaningInfoChannel();
          }catch (InsufficientPermissionException e) {
            logger.info("Error in a infochannel when cleaning : {}", e.getMessage());

          }catch (Exception e) {
            logger.warn("An unexpected error when cleaning info channel has occure.", e);
          }

          clearLoadingEmote();
        } else {
          server.setInfoChannel(null);
          server.setControlePannel(new ControlPannel());
        }
      }
    } catch(NullPointerException e) {
      logger.info("The Thread has crashed normally because of deletion of infoChannel :", e);
    }catch (InsufficientPermissionException e) {
      logger.info("Permission {} missing for infochannel in the guild {}, try to autofix the issue... (Low chance to work)",
          e.getPermission().getName(), server.getGuild().getName());
      try {
        PermissionOverride permissionOverride = server.getInfoChannel()
            .putPermissionOverride(server.getGuild().getMember(Zoe.getJda().getSelfUser())).complete();

        permissionOverride.getManager().grant(e.getPermission()).complete();
        logger.info("Autofix complete !");
      }catch(Exception e1) {
        logger.info("Autofix fail ! Error message : {} ", e1.getMessage());
      }
    } catch(Exception e) {
      logger.warn("The thread got a unexpected error :", e);
    } finally {
      server.setLastRefresh(DateTime.now());
      ServerData.getServersIsInTreatment().put(server.getGuild().getId(), false);
    }
  }

  private void cleanInfoChannelCache() {
    List<Message> messagesToRemove = new ArrayList<>();
    for(Message partInfoPannel : server.getControlePannel().getInfoPanel()) {
      try {
        partInfoPannel.addReaction("U+23F3").complete();
      }catch(ErrorResponseException e) {
        messagesToRemove.add(partInfoPannel);
      }
    }
    for(Message messageToRemove : messagesToRemove) {
      server.getControlePannel().getInfoPanel().remove(messageToRemove);
    }
  }

  private void clearLoadingEmote() {
    for(Message messageToClearReaction : server.getControlePannel().getInfoPanel()) {

      Message retrievedMessage;
      try {
        retrievedMessage = server.getInfoChannel().retrieveMessageById(messageToClearReaction.getId()).complete();
      } catch (ErrorResponseException e) {
        logger.warn("Error when deleting loading emote : {}", e.getMessage(), e);
        continue;
      }

      if(retrievedMessage != null) {
        for(MessageReaction messageReaction : retrievedMessage.getReactions()) {
          try {
            messageReaction.removeReaction(Zoe.getJda().getSelfUser()).queue();
          } catch (ErrorResponseException e) {
            logger.warn("Error when removing reaction : {}", e.getMessage(), e);
          }
        }
      }
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
      if(!messageToCheck.getTimeCreated().isBefore(OffsetDateTime.now().minusHours(1)) || server.getControlePannel().getInfoPanel().contains(messageToCheck)) {
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

    createInfoCards(controlPannel);
  }

  private void deleteOlderInfoCards() {
    List<InfoCard> cardsToRemove = new ArrayList<>();

    for(int i = 0; i < server.getControlePannel().getInfoCards().size(); i++) {
      InfoCard card = server.getControlePannel().getInfoCards().get(i);

      if(card.getCreationTime().plusMinutes(INFO_CARDS_MINUTES_LIVE_TIME).isBeforeNow()) {

        checkIfGameLongerExist(cardsToRemove, card);

      }
    }

    for(int i = 0; i < cardsToRemove.size(); i++) {
      try {
        removeMessage(cardsToRemove.get(i).getMessage());
        removeMessage(cardsToRemove.get(i).getTitle());

        server.getControlePannel().getInfoCards().remove(cardsToRemove.get(i));
      } catch(Exception e) {
        logger.warn("Impossible to delete message, retry next time : {}", e.getMessage(), e);
      }
    }
  }

  private void checkIfGameLongerExist(List<InfoCard> cardsToRemove, InfoCard card) {
    if(!card.getPlayers().isEmpty()) {
      Player player = card.getPlayers().get(0);
      if(!player.getLeagueAccountsInTheGivenGame(card.getCurrentGameInfo()).isEmpty()) {

        LeagueAccount account = player.getLeagueAccountsInTheGivenGame(card.getCurrentGameInfo()).get(0);

        if(account.getCurrentGameInfo() == null || account.getCurrentGameInfo().getGameId() != card.getCurrentGameInfo().getGameId()) {
          cardsToRemove.add(card);
        }

      }else {
        cardsToRemove.add(card);
      }
    }else {
      cardsToRemove.add(card);
    }
  }

  private void removeMessage(Message message) {
    try {
      message.delete().complete();
    } catch(ErrorResponseException e) {
      if(e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
        logger.info("Message already deleted : {}", e.getMessage());
      } else {
        logger.warn("Unhandle error : {}", e.getMessage());
        throw e;
      }
    }
  }

  private void createInfoCards(TextChannel controlPanel) {

    ArrayList<LeagueAccount> accountAlreadyGenerated = new ArrayList<>();

    for(LeagueAccount account : server.getAllAccountsOfTheServer()) {

      if(!accountAlreadyGenerated.contains(account)) {

        CurrentGameInfo currentGameInfo = account.getCurrentGameInfo();

        if(currentGameInfo != null && !server.getCurrentGamesIdAlreadySended().contains(currentGameInfo.getGameId())) {
          accountAlreadyGenerated.addAll(InfoPanelRefresherUtil.checkIfOthersAccountsInKnowInTheMatch(currentGameInfo, server));
          server.getCurrentGamesIdAlreadySended().add(currentGameInfo.getGameId());
          ServerData.getInfocardsGenerator().execute(new InfoCardsWorker(server, controlPanel, account, currentGameInfo));
        }
      }
    }
  }

  private String refreshPannel() {

    final List<Team> teamList = server.getAllPlayerTeams();
    final StringBuilder stringMessage = new StringBuilder();

    stringMessage.append("__**Information Panel**__\n \n");

    for(Team team : teamList) {

      if(teamList.size() != 1) {
        stringMessage.append("**" + team.getName() + "**\n \n");
      }

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
