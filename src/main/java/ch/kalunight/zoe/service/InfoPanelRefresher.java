package ch.kalunight.zoe.service;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.model.player_data.Team;
import ch.kalunight.zoe.repositories.GameInfoCardRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.TeamRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.constant.CallPriority;

public class InfoPanelRefresher implements Runnable {

  private static final int INFO_CARDS_MINUTES_LIVE_TIME = 20;

  private static final Logger logger = LoggerFactory.getLogger(InfoPanelRefresher.class);

  private static final Gson gson = new GsonBuilder().create();

  private DTO.Server server;

  private List<DTO.GameInfoCard> gameInfoCards;
  
  private TextChannel infochannel;

  private Guild guild;

  private boolean needToWait = false;

  public InfoPanelRefresher(DTO.Server server) {
    this.server = server;
    guild = Zoe.getJda().getGuildById(server.serv_guildId);
  }

  public InfoPanelRefresher(DTO.Server server, boolean needToWait) {
    this.server = server;
    this.needToWait = needToWait;
    guild = Zoe.getJda().getGuildById(server.serv_guildId);
  }


  @Override
  public void run() {
    try {

      DTO.InfoChannel infoChannelDTO = InfoChannelRepository.getInfoChannel(server.serv_guildId);
      if(infochannel != null) {
        infochannel = guild.getTextChannelById(infoChannelDTO.infochannel_channelid);
        gameInfoCards = GameInfoCardRepository.getGameInfoCard(server.serv_guildId);
      }

      if(infochannel != null) {

        if(needToWait) {
          TimeUnit.SECONDS.sleep(3);
        }

        List<DTO.Player> playersDTO = PlayerRepository.getPlayers(server.serv_guildId);

        cleanRegisteredPlayerNoLongerInGuild(playersDTO);
        refreshAllLeagueAccount(playersDTO);
        clearOldMatchOfSendedGamesIdList(playersDTO);
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
              server.getControlePannel().getInfoPanel().add(server.getInfoChannel()
                  .sendMessage(LanguageManager.getText(server.getLangage(), "loading")).complete());
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
      logger.warn("The Thread has crashed prbably normally because of deletion of infoChannel :", e);
    }catch (InsufficientPermissionException e) {
      logger.debug("Permission {} missing for infochannel in the guild {}, try to autofix the issue... (Low chance to work)",
          e.getPermission().getName(), server.getGuild().getName());
      try {
        PermissionOverride permissionOverride = server.getInfoChannel()
            .putPermissionOverride(server.getGuild().getMember(Zoe.getJda().getSelfUser())).complete();

        permissionOverride.getManager().grant(e.getPermission()).complete();
        logger.debug("Autofix complete !");
      }catch(Exception e1) {
        logger.debug("Autofix fail ! Error message : {} ", e1.getMessage());
      }
      
    } catch (SQLException e) {
      logger.error("SQL Exception when refresh the infopanel !", e);
    } catch(Exception e) {
      logger.error("The thread got a unexpected error :", e);
    } finally {
      server.setLastRefresh(DateTime.now());
      ServerData.getServersIsInTreatment().put(server.getGuild().getId(), false);
    }
  }

  public void clearOldMatchOfSendedGamesIdList(List<DTO.Player> players) throws SQLException {

    final List<DTO.LeagueAccount> allCurrentGamesOfPlayers = new ArrayList<>();
    for(DTO.Player player : players) {
      for(DTO.LeagueAccount leagueAccount : player.leagueAccounts) {
        if(leagueAccount.leagueAccount_currentGame != null) {
          allCurrentGamesOfPlayers.add(leagueAccount);
        }
      }
    }

    final Iterator<DTO.LeagueAccount> iterator = allCurrentGamesOfPlayers.iterator();

    final List<Long> gameIdToSave = new ArrayList<>();

    while(iterator.hasNext()) {

      final DTO.LeagueAccount currentGameInfo = iterator.next();

      if(currentGameInfo != null) {
        gameIdToSave.add(currentGameInfo.leagueAccount_currentGame.getGameId());
      }
    }

    Iterator<DTO.GameInfoCard> gameInfoCardsIterator = gameInfoCards.iterator();

    while(gameInfoCardsIterator.hasNext()) {
      DTO.GameInfoCard actualgameInfoCard = gameInfoCardsIterator.next();
      List<DTO.LeagueAccount> leaguesAccountInGames = LeagueAccountRepository.getLeaguesAccountsWithGameCardsId(actualgameInfoCard.gamecard_id);
      DTO.LeagueAccount leagueAccount = leaguesAccountInGames.get(0);
      
      if(!gameIdToSave.contains(leagueAccount.leagueAccount_currentGame.getGameId())) {
        gameInfoCardsIterator.remove();
        GameInfoCardRepository.deleteGameInfoCardsWithId(actualgameInfoCard.gamecard_id);
      }
    }
  }

  private void refreshAllLeagueAccount(List<DTO.Player> playersDTO) throws SQLException, RiotApiException {
    for(DTO.Player player : playersDTO) {
      player.leagueAccounts =
          LeagueAccountRepository.getLeaguesAccounts(server.serv_guildId, player.player_discordId);

      for(DTO.LeagueAccount leagueAccount : player.leagueAccounts) {
        CurrentGameInfo currentGame = Zoe.getRiotApi().getActiveGameBySummoner(
            leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId, CallPriority.NORMAL);

        leagueAccount.leagueAccount_currentGame = gson.toJson(currentGame);
        LeagueAccountRepository.updateAccountWithId(
            leagueAccount.leagueAccount_id, leagueAccount.leagueAccount_currentGame);
      }
    }
  }

  private void cleanInfoChannelCache() {
    List<Message> messagesToRemove = new ArrayList<>();
    for(Message partInfoPannel : server.getControlePannel().getInfoPanel()) {
      try {
        partInfoPannel.addReaction("U+23F3").complete();
      }catch(ErrorResponseException e) {
        //Try to check in a less pretty way
        try {
          server.getInfoChannel().retrieveMessageById(partInfoPannel.getId()).complete();
        } catch (ErrorResponseException e1) {
          messagesToRemove.add(partInfoPannel);
        }
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


  private void cleanRegisteredPlayerNoLongerInGuild(List<DTO.Player> listPlayers) throws SQLException {

    Iterator<DTO.Player> iter = listPlayers.iterator();

    while (iter.hasNext()) {
      DTO.Player player = iter.next();
      if(guild.getMemberById(player.user.getId()) == null) {
        iter.remove();
        PlayerRepository.updateTeamOfPlayerDefineNull(player.player_id);
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

    stringMessage.append("__**" + LanguageManager.getText(server.getLangage(), "informationPanelTitle") + "**__\n \n");

    for(Team team : teamList) {

      if(teamList.size() != 1) {
        stringMessage.append("**" + team.getName() + "**\n \n");
      }

      List<Player> playersList = team.getPlayers();

      for(Player player : playersList) {

        List<LeagueAccount> leagueAccounts = player.getLeagueAccountsInGame();

        if(leagueAccounts.isEmpty()) {
          stringMessage.append(player.getDiscordUser().getAsMention() + " : " 
              + LanguageManager.getText(server.getLangage(), "informationPanelNotInGame") + " \n");
        }else if (leagueAccounts.size() == 1) {
          stringMessage.append(player.getDiscordUser().getAsMention() + " : " 
              + InfoPanelRefresherUtil.getCurrentGameInfoStringForOneAccount(leagueAccounts.get(0), server.getLangage()) + "\n");
        }else {
          stringMessage.append(player.getDiscordUser().getAsMention() + " : " 
              + LanguageManager.getText(server.getLangage(), "informationPanelMultipleAccountInGame") + "\n"
              + InfoPanelRefresherUtil.getCurrentGameInfoStringForMultipleAccounts(leagueAccounts, server.getLangage()));
        }
      }
      stringMessage.append(" \n");
    }

    stringMessage.append(LanguageManager.getText(server.getLangage(), "informationPanelRefreshedTime"));

    return stringMessage.toString();
  }


}
