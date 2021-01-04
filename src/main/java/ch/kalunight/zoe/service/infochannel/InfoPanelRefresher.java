package ch.kalunight.zoe.service.infochannel;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.ComparableMessage;
import ch.kalunight.zoe.model.RefreshPhase;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.InfoChannel;
import ch.kalunight.zoe.model.dto.DTO.InfoPanelMessage;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.RankHistoryChannel;
import ch.kalunight.zoe.model.dto.DTO.ServerStatus;
import ch.kalunight.zoe.model.dto.GameInfoCardStatus;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.CurrentGameInfoRepository;
import ch.kalunight.zoe.repositories.GameInfoCardRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.repositories.LeaderboardRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.RankHistoryChannelRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.repositories.ServerStatusRepository;
import ch.kalunight.zoe.repositories.TeamRepository;
import ch.kalunight.zoe.service.ServerChecker;
import ch.kalunight.zoe.service.rankchannel.RankedChannelLoLRefresher;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import ch.kalunight.zoe.util.TreatedPlayer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.constant.Platform;

public class InfoPanelRefresher implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(InfoPanelRefresher.class);

  private static final AtomicLong nbrServerRefreshedLast2Minutes = new AtomicLong(0);

  private DTO.Server server;

  private TextChannel infochannel;

  private RankHistoryChannel rankChannel;

  private Guild guild;
  
  private boolean forceRefreshCache;

  public InfoPanelRefresher(DTO.Server server, boolean forceRefreshCache) {
    this.server = server;
    this.forceRefreshCache = forceRefreshCache; 
    guild = Zoe.getJda().getGuildById(server.serv_guildId);
  }

  @Override
  public void run() {
    try {

      nbrServerRefreshedLast2Minutes.incrementAndGet();

      if(guild == null) {
        return;
      }

      DTO.InfoChannel infoChannelDTO = InfoChannelRepository.getInfoChannel(server.serv_guildId);
      if(infoChannelDTO != null) {
        infochannel = guild.getTextChannelById(infoChannelDTO.infochannel_channelid);
      }

      ServerConfiguration configuration = ConfigRepository.getServerConfiguration(guild.getIdLong());

      List<DTO.Player> playersDTO = PlayerRepository.getPlayers(server.serv_guildId);

      InfoPanelRefresherUtil.cleanRegisteredPlayerNoLongerInGuild(guild, playersDTO);

      if(infochannel != null) {
        cleanOldInfoChannelMessage();
      }

      rankChannel = RankHistoryChannelRepository.getRankHistoryChannel(server.serv_guildId);

      List<TreatPlayerWorker> playersToTreat = new ArrayList<>();

      List<TreatedPlayer> treatedPlayers = new ArrayList<>();

      if(infochannel != null || rankChannel != null) {

        for(Player player : playersDTO) {
          List<LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithPlayerID(server.serv_guildId, player.player_id);

          TreatPlayerWorker playerWorker = new TreatPlayerWorker(server, player, leaguesAccounts, rankChannel, configuration, forceRefreshCache);

          playersToTreat.add(playerWorker);
          if(!leaguesAccounts.isEmpty()) {
            ServerThreadsManager.getInfochannelHelperThread(leaguesAccounts.get(0).leagueAccount_server).execute(playerWorker);
          }else {
            // When there's no accounts, we go in a not really used threadpool
            ServerThreadsManager.getInfochannelHelperThread(Platform.OCE).execute(playerWorker);
          }
        }

        TreatPlayerWorker.awaitAll(playersToTreat);

        executeRankChannel(playersToTreat);

        Map<CurrentGameInfo, List<LeagueAccount>> leaguesAccountsPerGameWaitingCreation = Collections.synchronizedMap(new HashMap<CurrentGameInfo, List<LeagueAccount>>());

        Map<DTO.CurrentGameInfo, List<LeagueAccount>> leaguesAccountsPerGameWaitingDeletion = Collections.synchronizedMap(new HashMap<DTO.CurrentGameInfo, List<LeagueAccount>>());

        loadTreatedPlayers(playersToTreat, treatedPlayers, leaguesAccountsPerGameWaitingCreation,
            leaguesAccountsPerGameWaitingDeletion);

        applyDBChange(leaguesAccountsPerGameWaitingCreation, leaguesAccountsPerGameWaitingDeletion);
      }

      if(infochannel != null && guild != null) {

        cleanUnlinkInfoCardAndCurrentGame();
        refreshGameCardStatus();

        refreshInfoPanel(infoChannelDTO, configuration, treatedPlayers);

        if(ConfigRepository.getServerConfiguration(server.serv_guildId).getInfoCardsOption().isOptionActivated()) {
          createMissingInfoCard();
          treathGameCardWithStatus();
        }

        cleanInfoChannel();
        clearLoadingEmote();
      }else if(infoChannelDTO != null && infochannel == null) {
        InfoChannelRepository.deleteInfoChannel(server);
      }
    }catch (InsufficientPermissionException e) {
      try {
        logger.info("Permission {} missing for infochannel in the guild {}, try to autofix the issue... (Low chance to work)",
            e.getPermission().getName(), guild.getName());

        PermissionOverride permissionOverride = infochannel
            .putPermissionOverride(guild.retrieveMember(Zoe.getJda().getSelfUser()).complete()).complete();

        permissionOverride.getManager().grant(e.getPermission()).complete();
        logger.info("Autofix complete !");
      }catch(Exception e1) {
        logger.info("Autofix fail ! Error message : {} ", e1.getMessage());
      }

    } catch (SQLException e) {
      logger.error("SQL Exception when refresh the infopanel in the guild id {} ! SQL State : {}", guild.getIdLong(), e.getSQLState(), e);
    } catch(Exception e) {
      logger.error("The thread got a unexpected error in the guild id {} (The channel got probably deleted when the refresh append)", guild.getIdLong(), e);
    } finally {
      updateServerStatus();
    }
  }

  private void executeRankChannel(List<TreatPlayerWorker> playersToTreat) {

    for(TreatPlayerWorker playerWorker : playersToTreat) {
      if(playerWorker.getTreatedPlayer() != null) {
        for(RankedChannelLoLRefresher rankChannelRefresher : playerWorker.getTreatedPlayer().getRankChannelsToProcess()) {
          ServerThreadsManager.getRankedMessageGenerator().execute(rankChannelRefresher);
        }
      }
    }
  }

  private void applyDBChange(Map<CurrentGameInfo, List<LeagueAccount>> leaguesAccountsPerGameWaitingCreation,
      Map<DTO.CurrentGameInfo, List<LeagueAccount>> leaguesAccountsPerGameWaitingDeletion) throws SQLException {
    for(Entry<DTO.CurrentGameInfo, List<LeagueAccount>> gameToDelete : leaguesAccountsPerGameWaitingDeletion.entrySet()) {
      //Current game will be deleted in cleanUnlinkInfoCardAndCurrentGame()
      for(LeagueAccount accountToUnlink : gameToDelete.getValue()) {
        LeagueAccountRepository.updateAccountCurrentGameWithAccountId(accountToUnlink.leagueAccount_id, 0);
      }
    }

    for(Entry<CurrentGameInfo, List<LeagueAccount>> gameToCreate : leaguesAccountsPerGameWaitingCreation.entrySet()) {
      Long currentGameId = null;
      for(LeagueAccount leagueAccount : gameToCreate.getValue()) {
        if(currentGameId == null) {
          currentGameId = CurrentGameInfoRepository.createCurrentGame(gameToCreate.getKey(), leagueAccount);
        }else {
          LeagueAccountRepository.updateAccountCurrentGameWithAccountId(leagueAccount.leagueAccount_id, currentGameId);
        }
      }
    }
  }

  private void loadTreatedPlayers(List<TreatPlayerWorker> playersToTreat, List<TreatedPlayer> treatedPlayers,
      Map<CurrentGameInfo, List<LeagueAccount>> leaguesAccountsPerGameWaitingCreation,
      Map<DTO.CurrentGameInfo, List<LeagueAccount>> leaguesAccountsPerGameWaitingDeletion) {
    for(TreatPlayerWorker treatPlayerWorker : playersToTreat) {
      TreatedPlayer treatedPlayer = treatPlayerWorker.getTreatedPlayer();

      if(treatedPlayer != null) {
        treatedPlayers.add(treatedPlayer);

        loadGameToCreate(leaguesAccountsPerGameWaitingCreation, treatedPlayer);

        loadGameToDelete(leaguesAccountsPerGameWaitingDeletion, treatedPlayer);
      }else {
        logger.error("Error while loading a player! Player id {} | Guild discord id {}", treatPlayerWorker.getPlayer().getUser().getId(), treatPlayerWorker.getServer().serv_guildId);
      }
    }
  }

  private void loadGameToDelete(Map<DTO.CurrentGameInfo, List<LeagueAccount>> leaguesAccountsPerGameWaitingDeletion,
      TreatedPlayer treatedPlayer) {
    for(Entry<DTO.CurrentGameInfo, LeagueAccount> gameToDelete : treatedPlayer.getGamesToDelete().entrySet()) {
      boolean gameAlreadyAdded = false;
      for(Entry<DTO.CurrentGameInfo, List<LeagueAccount>> gameAlreadyWaitingToBeDeleted : leaguesAccountsPerGameWaitingDeletion.entrySet()) {

        if(gameAlreadyWaitingToBeDeleted.getKey().currentgame_id == gameToDelete.getKey().currentgame_id) {
          gameAlreadyAdded = true;
          gameAlreadyWaitingToBeDeleted.getValue().add(gameToDelete.getValue());
        }
      }

      if(!gameAlreadyAdded) {
        List<LeagueAccount> leagueAccountsOfTheGame = new ArrayList<>();
        leagueAccountsOfTheGame.add(gameToDelete.getValue());
        leaguesAccountsPerGameWaitingDeletion.put(gameToDelete.getKey(), leagueAccountsOfTheGame);
      }
    }
  }

  private void loadGameToCreate(Map<CurrentGameInfo, List<LeagueAccount>> leaguesAccountsPerGameWaitingCreation,
      TreatedPlayer treatedPlayer) {
    Set<Entry<CurrentGameInfo, LeagueAccount>> gameToCreatePerAccount = treatedPlayer.getGamesToCreate().entrySet();
    for(Entry<CurrentGameInfo, LeagueAccount> gamePerAccount : gameToCreatePerAccount) {
      boolean gameAlreadyAdded = false;
      for(Entry<CurrentGameInfo, List<LeagueAccount>> gamesAlreadyWaitingForCreation : leaguesAccountsPerGameWaitingCreation.entrySet()) {
        if(gamePerAccount.getKey().getGameId() == gamesAlreadyWaitingForCreation.getKey().getGameId()) {
          gameAlreadyAdded = true;

          if(!gamesAlreadyWaitingForCreation.getValue().isEmpty() 
              && gamesAlreadyWaitingForCreation.getValue().get(0).leagueAccount_server == gamePerAccount.getValue().leagueAccount_server) {
            gamesAlreadyWaitingForCreation.getValue().add(gamePerAccount.getValue());
          }
        }
      }

      if(!gameAlreadyAdded) {
        List<LeagueAccount> leagueAccountsOfTheGame = new ArrayList<>();
        leagueAccountsOfTheGame.add(gamePerAccount.getValue());
        leaguesAccountsPerGameWaitingCreation.put(gamePerAccount.getKey(), leagueAccountsOfTheGame);
      }
    }
  }

  private void updateServerStatus() {
    ServerStatus status;
    try {
      status = ServerStatusRepository.getServerStatus(server.serv_guildId);
    } catch (SQLException e) {
      logger.error("SQL Exception when getting status ! Retry in 1 seconds ... | SQL State : {}", e.getSQLState(), e);
      try {
        TimeUnit.SECONDS.sleep(1);
        updateServerStatus();
      } catch (InterruptedException e1) {
        logger.error("InterruptedException while updating timeStamp and treatment !", e);
        Thread.currentThread().interrupt();
      }
      return;
    }

    try {
      ServerRepository.updateTimeStamp(server.serv_guildId, LocalDateTime.now());
      ServerStatusRepository.updateInTreatment(status.servstatus_id, false);
    } catch(SQLException e) {
      logger.error("SQL Exception when updating timeStamp and treatment ! Retry in 1 seconds ... | SQL State : {}", e.getSQLState(), e);
      try {
        TimeUnit.SECONDS.sleep(1);
        updateServerStatus();
      } catch (InterruptedException e1) {
        logger.error("InterruptedException while updating timeStamp and treatment !", e);
        Thread.currentThread().interrupt();
      }
    }
  }

  private void cleanInfoChannel() {
    try {
      cleaningInfoChannel();
    }catch (InsufficientPermissionException e) {
      logger.info("Error in a infochannel when cleaning : {}", e.getMessage());

    }catch (Exception e) {
      logger.warn("An unexpected error when cleaning info channel has occure.", e);
    }
  }

  private void cleanUnlinkInfoCardAndCurrentGame() throws SQLException {
    List<DTO.CurrentGameInfo> currentGamesInfo = CurrentGameInfoRepository.getCurrentGamesWithoutLinkAccounts(server.serv_guildId);

    for(DTO.CurrentGameInfo currentGame : currentGamesInfo) {
      DTO.GameInfoCard gameCard = GameInfoCardRepository.getGameInfoCardsWithCurrentGameId(server.serv_guildId, currentGame.currentgame_id);

      if(gameCard.gamecard_infocardmessageid != 0) {
        retrieveAndRemoveMessage(gameCard.gamecard_infocardmessageid);
      }
      GameInfoCardRepository.deleteGameInfoCardsWithId(gameCard.gamecard_id);
      CurrentGameInfoRepository.deleteCurrentGame(currentGame, server);
    }
  }

  private void treathGameCardWithStatus() throws SQLException {

    List<DTO.GameInfoCard> gameInfoCards = GameInfoCardRepository.getGameInfoCards(server.serv_guildId);

    for(DTO.GameInfoCard gameInfoCard : gameInfoCards) {
      switch(gameInfoCard.gamecard_status) {
      case IN_CREATION:
        GameInfoCardRepository.updateGameInfoCardStatusWithId(gameInfoCard.gamecard_id, GameInfoCardStatus.IN_TREATMENT);

        List<DTO.LeagueAccount> accountsLinked = LeagueAccountRepository
            .getLeaguesAccountsWithGameCardsId(gameInfoCard.gamecard_id);

        DTO.LeagueAccount account = accountsLinked.get(0);

        DTO.CurrentGameInfo currentGame = CurrentGameInfoRepository.getCurrentGameWithLeagueAccountID(account.leagueAccount_id);

        ServerThreadsManager.getInfocardsGenerator().execute(
            new InfoCardsWorker(server, infochannel, accountsLinked.get(0), currentGame, gameInfoCard, forceRefreshCache));
        break;
      case IN_WAIT_OF_DELETING:
        GameInfoCardRepository.deleteGameInfoCardsWithId(gameInfoCard.gamecard_id);
        deleteDiscordInfoCard(server.serv_guildId, gameInfoCard);
        break;
      default:
        break;
      }
    }
  }

  private void refreshInfoPanel(DTO.InfoChannel infoChannelDTO, ServerConfiguration configuration, List<TreatedPlayer> treatedPlayers)
      throws SQLException {
    ArrayList<String> infoPanels = CommandEvent.splitMessage(refreshPannel(configuration, treatedPlayers));

    List<DTO.InfoPanelMessage> infoPanelMessages = InfoChannelRepository.getInfoPanelMessages(server.serv_guildId);

    removeDeletedMessageFromTheDB(infoPanelMessages);

    checkMessageDisplaySync(infoPanelMessages, infoChannelDTO); 

    infoPanelMessages = InfoChannelRepository.getInfoPanelMessages(server.serv_guildId);

    if(infoPanels.size() < infoPanelMessages.size()) {
      int nbrMessageToDelete = infoPanelMessages.size() - infoPanels.size();
      for(int i = 0; i < nbrMessageToDelete; i++) {
        DTO.InfoPanelMessage infoPanelMessage = infoPanelMessages.get(i);
        Message message = infochannel.retrieveMessageById(infoPanelMessage.infopanel_messageId).complete();
        message.delete().queue();
        InfoChannelRepository.deleteInfoPanelMessage(infoPanelMessage.infopanel_id);
      }

    } else {
      int nbrMessageToAdd = infoPanels.size() - infoPanelMessages.size();
      for(int i = 0; i < nbrMessageToAdd; i++) {
        Message message = infochannel.sendMessage(LanguageManager.getText(server.serv_language, "loading")).complete();
        InfoChannelRepository.createInfoPanelMessage(infoChannelDTO.infoChannel_id, message.getIdLong());
      }
    }

    infoPanelMessages.clear();
    infoPanelMessages.addAll(InfoChannelRepository.getInfoPanelMessages(server.serv_guildId));

    infoPanelMessages = orderInfoPanelMessagesByTime(infoPanelMessages);

    for(int i = 0; i < infoPanels.size(); i++) {
      DTO.InfoPanelMessage infoPanel = infoPanelMessages.get(i);
      infochannel.retrieveMessageById(infoPanel.infopanel_messageId).complete().editMessage(infoPanels.get(i)).queue();
    }
  }

  private void removeDeletedMessageFromTheDB(List<InfoPanelMessage> infoPanelMessages) throws SQLException {

    List<InfoPanelMessage> infopanelMessagesDeleted = new ArrayList<>();

    for(InfoPanelMessage messageToTest : infoPanelMessages) {
      try {
        infochannel.retrieveMessageById(messageToTest.infopanel_messageId).complete();
      }catch(ErrorResponseException e) {
        if(e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
          InfoChannelRepository.deleteInfoPanelMessage(messageToTest.infopanel_id);
          infopanelMessagesDeleted.add(messageToTest);
        }
      }
    }

    infoPanelMessages.removeAll(infopanelMessagesDeleted);
  }

  private void checkMessageDisplaySync(List<InfoPanelMessage> infoPanelMessages, InfoChannel infochannelDTO) {

    List<Message> messagesToCheck = new ArrayList<>();
    for(InfoPanelMessage messageToLoad : infoPanelMessages) {
      Message message = infochannel.retrieveMessageById(messageToLoad.infopanel_messageId).complete();
      messagesToCheck.add(message);
    }

    boolean needToResend = false;

    List<Message> orderedMessage = orderMessagesByTime(messagesToCheck);

    for(Message messageToCompare : orderedMessage) {
      for(Message secondMessageToCompare : orderedMessage) {
        if(messageToCompare.getIdLong() != secondMessageToCompare.getIdLong()) {
          if(messageToCompare.getTimeCreated().until(secondMessageToCompare.getTimeCreated(), ChronoUnit.MINUTES) > 5) {
            needToResend = true;
          }
        }
      }
    }

    if(needToResend) {
      int messageNeeded = infoPanelMessages.size();

      for(InfoPanelMessage infoPanelMessageToDelete : infoPanelMessages) {
        Message messageToDelete = infochannel.retrieveMessageById(infoPanelMessageToDelete.infopanel_messageId).complete();

        try {
          InfoChannelRepository.deleteInfoPanelMessage(infoPanelMessageToDelete.infopanel_id);
          messageToDelete.delete().queue();
        } catch (SQLException e) {
          logger.warn("Error when deleting infoPanel message in db. Try again next refresh");
        }
      }

      for(int i = 0; i < messageNeeded; i++) {
        Message message = infochannel.sendMessage("**" + LanguageManager.getText(server.serv_language, "infopanelMessageReSendMessage") + "**").complete();
        try {
          InfoChannelRepository.createInfoPanelMessage(infochannelDTO.infoChannel_id, message.getIdLong());
        } catch (SQLException e) {
          logger.warn("Error while creating new info Panel Message. Try again in the process.");
        }
      }
    }
  }

  private List<Message> orderMessagesByTime(List<Message> messagesToCheck) {

    List<ComparableMessage> messagesToOrder = new ArrayList<>();

    for(Message message : messagesToCheck) {
      messagesToOrder.add(new ComparableMessage(message));
    }

    Collections.sort(messagesToOrder); 

    List<Message> messagesOrdered = new ArrayList<>();
    for(ComparableMessage messageOrdered : messagesToOrder) {
      messagesOrdered.add(messageOrdered.getMessage());
    }
    return messagesOrdered;
  }

  private List<InfoPanelMessage> orderInfoPanelMessagesByTime(List<InfoPanelMessage> infoPanelMessages) {

    List<Message> baseMessageToOrder = new ArrayList<>();
    for(InfoPanelMessage infoPanelMessage : infoPanelMessages) {
      baseMessageToOrder.add(infochannel.retrieveMessageById(infoPanelMessage.infopanel_messageId).complete());
    }

    List<ComparableMessage> messagesToOrder = new ArrayList<>();

    for(Message message : baseMessageToOrder) {
      messagesToOrder.add(new ComparableMessage(message));
    }

    Collections.sort(messagesToOrder);

    List<InfoPanelMessage> messagesOrdered = new ArrayList<>();
    for(ComparableMessage messageOrdered : messagesToOrder) {
      for(InfoPanelMessage infoPanelMessage : infoPanelMessages) {
        if(messageOrdered.getMessage().getIdLong() == infoPanelMessage.infopanel_messageId) {
          messagesOrdered.add(infoPanelMessage);
        }
      }
    }

    return messagesOrdered;
  }

  private List<DTO.GameInfoCard> refreshGameCardStatus() throws SQLException {
    List<DTO.GameInfoCard> gameInfoCards = GameInfoCardRepository.getGameInfoCards(server.serv_guildId);

    for(DTO.GameInfoCard gameInfoCard : gameInfoCards) {
      if(gameInfoCard.gamecard_status == GameInfoCardStatus.IN_WAIT_OF_MATCH_ENDING) {
        if(gameInfoCard.gamecard_fk_currentgame == 0) {
          GameInfoCardRepository.updateGameInfoCardStatusWithId(
              gameInfoCard.gamecard_id, GameInfoCardStatus.IN_WAIT_OF_DELETING);
          gameInfoCard.gamecard_status = GameInfoCardStatus.IN_WAIT_OF_DELETING;
        }
      }
    }
    return gameInfoCards;
  }

  private void createMissingInfoCard() throws SQLException {
    List<DTO.CurrentGameInfo> currentGamesWithoutCard = 
        CurrentGameInfoRepository.getCurrentGameWithoutLinkWithGameCardAndWithGuildId(server.serv_guildId);

    List<DTO.CurrentGameInfo> alreadyGeneratedGames = new ArrayList<>();

    DTO.InfoChannel infochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);

    for(DTO.CurrentGameInfo currentGame : currentGamesWithoutCard) {
      boolean alreadyGenerated = false;
      for(DTO.CurrentGameInfo alreadyGeneratedGame : alreadyGeneratedGames) {
        if(alreadyGeneratedGame.currentgame_currentgame.getGameId() == currentGame.currentgame_currentgame.getGameId()) {
          alreadyGenerated = true;
        }
      }


      if(!alreadyGenerated) {
        GameInfoCardRepository.createGameCards(
            infochannel.infoChannel_id, currentGame.currentgame_id, GameInfoCardStatus.IN_CREATION);
        alreadyGeneratedGames.add(currentGame);
      }


      linkAccountWithGameCard(currentGame);
    }
  }

  private void linkAccountWithGameCard(DTO.CurrentGameInfo currentGame) throws SQLException {
    DTO.GameInfoCard gameCard = GameInfoCardRepository.
        getGameInfoCardsWithCurrentGameId(server.serv_guildId, currentGame.currentgame_id);

    if(gameCard == null) {
      return;
    }

    List<DTO.LeagueAccount> leaguesAccountInTheGame = 
        LeagueAccountRepository.getLeaguesAccountsWithCurrentGameId(currentGame.currentgame_id);

    for(DTO.LeagueAccount leagueAccount : leaguesAccountInTheGame) {
      if(leagueAccount != null) {
        LeagueAccountRepository.updateAccountGameCardWithAccountId(leagueAccount.leagueAccount_id, gameCard.gamecard_id);
      }
    }
  }

  private void deleteDiscordInfoCard(long guildId, DTO.GameInfoCard gameCard) throws SQLException {
    List<DTO.LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithGameCardsId(gameCard.gamecard_id);

    for(DTO.LeagueAccount leagueAccount: leaguesAccounts) {
      LeagueAccountRepository.updateAccountCurrentGameWithAccountId(leagueAccount.leagueAccount_id, 0);
    }

    GameInfoCardRepository.deleteGameInfoCardsWithId(gameCard.gamecard_id);

    retrieveAndRemoveMessage(gameCard.gamecard_infocardmessageid);
  }

  private void retrieveAndRemoveMessage(long messageId) {
    try {
      removeMessage(infochannel.retrieveMessageById(messageId).complete());
    }catch(ErrorResponseException e) {
      logger.info("The wanted info panel message doesn't exist anymore.");
    }
  }

  private void cleanOldInfoChannelMessage() throws SQLException {
    List<DTO.InfoPanelMessage> messagesToRemove = new ArrayList<>();
    List<DTO.InfoPanelMessage> infopanels = InfoChannelRepository.getInfoPanelMessages(server.serv_guildId);
    for(DTO.InfoPanelMessage partInfoPannel : infopanels) {
      try {
        Message message = infochannel.retrieveMessageById(partInfoPannel.infopanel_messageId).complete();
        message.addReaction("U+23F3").complete();
      }catch(ErrorResponseException e) {
        //Try to check in a less pretty way
        try {
          infochannel.retrieveMessageById(partInfoPannel.infopanel_messageId).complete();
        } catch (ErrorResponseException e1) {
          messagesToRemove.add(partInfoPannel);
        }
      }
    }
    for(DTO.InfoPanelMessage messageToRemove : messagesToRemove) {
      InfoChannelRepository.deleteInfoPanelMessage(messageToRemove.infopanel_id);
    }
  }

  private void clearLoadingEmote() throws SQLException {
    for(DTO.InfoPanelMessage messageToClearReaction : InfoChannelRepository.getInfoPanelMessages(server.serv_guildId)) {

      Message retrievedMessage;
      try {
        retrievedMessage = infochannel.retrieveMessageById(messageToClearReaction.infopanel_messageId).complete();
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

  private void cleaningInfoChannel() throws SQLException {

    List<Message> messagesToCheck = infochannel.getIterableHistory().stream()
        .limit(1000)
        .filter(m-> m.getAuthor().getId().equals(Zoe.getJda().getSelfUser().getId()))
        .collect(Collectors.toList());

    List<Message> messagesToDelete = new ArrayList<>();
    List<DTO.InfoPanelMessage> infoPanels = InfoChannelRepository.getInfoPanelMessages(server.serv_guildId);
    List<Long> messagesToNotDelete = new ArrayList<>();
    for(DTO.InfoPanelMessage infoPanel : infoPanels) {
      messagesToNotDelete.add(infoPanel.infopanel_messageId);
    }

    List<Leaderboard> leaderboards = LeaderboardRepository.getLeaderboardsWithGuildId(server.serv_guildId);
    for(Leaderboard leaderboard : leaderboards) {
      if(infochannel.getIdLong() == leaderboard.lead_message_channelId) {
        messagesToNotDelete.add(leaderboard.lead_message_id);
      }
    }

    for(Message messageToCheck : messagesToCheck) {
      if(!messageToCheck.getTimeCreated().isBefore(OffsetDateTime.now().minusHours(1)) || messagesToNotDelete.contains(messageToCheck.getIdLong())) {
        continue;
      }

      messagesToDelete.add(messageToCheck);
    }

    if(messagesToDelete.isEmpty()) {
      return;
    }

    if(messagesToDelete.size() > 1) {
      infochannel.purgeMessages(messagesToDelete);
    }else {
      for(Message messageToDelete : messagesToDelete) {
        messageToDelete.delete().queue();
      }
    }
  }

  private void removeMessage(Message message) {
    try {
      if(message != null) {
        message.delete().complete();
      }
    } catch(ErrorResponseException e) {
      if(e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
        logger.info("Message already deleted");
      } else {
        logger.warn("Unhandle error : {}", e.getMessage());
        throw e;
      }
    }
  }

  private String refreshPannel(ServerConfiguration configuration, List<TreatedPlayer> treatedPlayers) throws SQLException {

    final List<DTO.Team> teamList = TeamRepository.getTeamsByGuild(server.serv_guildId);
    final StringBuilder stringMessage = new StringBuilder();

    stringMessage.append("__**" + LanguageManager.getText(server.serv_language, "informationPanelTitle") + "**__\n \n");

    List<TreatedPlayer> playersNotInTeam = new ArrayList<>();
    playersNotInTeam.addAll(treatedPlayers);

    for(DTO.Team team : teamList) {
      generateATeam(treatedPlayers, stringMessage, playersNotInTeam, team, configuration);
    }

    if(!playersNotInTeam.isEmpty()) {
      if(teamList.isEmpty()) {
        for(TreatedPlayer playerToShow : playersNotInTeam) { //We can order player here
          stringMessage.append(playerToShow.getInfochannelMessage());
        }

        stringMessage.append("\n");
      }else {
        generateATeam(playersNotInTeam, stringMessage, null, 
            new DTO.Team(0, 0, LanguageManager.getText(server.serv_language, "teamNameOfPlayerWithoutTeam")), configuration);
      }
    }

    if(ServerChecker.getLastStatus().getRefreshPhase().equals(RefreshPhase.SMART_MOD)) {
      stringMessage.append(LanguageManager.getText(server.serv_language, "informationPanelSmartModEnable"));
    } else if (ServerChecker.getLastStatus().getRefreshPhase().equals(RefreshPhase.IN_EVALUATION_PHASE) 
        || ServerChecker.getLastStatus().getRefreshPhase().equals(RefreshPhase.IN_EVALUATION_PHASE_ON_ROAD) ) {
      stringMessage.append(LanguageManager.getText(server.serv_language, "informationPanelEvaluationMod"));
    }else {
      stringMessage.append(String.format(LanguageManager.getText(server.serv_language, "informationPanelRefreshedTime"), ServerChecker.getLastStatus().getRefresRatehInMinute().get()));
    }

    return stringMessage.toString();
  }

  private void generateATeam(List<TreatedPlayer> treatedPlayers, final StringBuilder stringMessage,
      List<TreatedPlayer> playersNotInTeam, DTO.Team team, ServerConfiguration config) throws SQLException {

    List<TreatedPlayer> playersInTeam = new ArrayList<>();
    int numberOfAccountRanked = 0;
    int totRankValue = 0;

    for(TreatedPlayer player : treatedPlayers) {
      if((player.getTeam() != null && player.getTeam().team_id == team.team_id) || team.team_id == 0) {
        if(team.team_id != 0) {
          playersNotInTeam.remove(player);
        }
        playersInTeam.add(player);
        try {
          totRankValue += player.getAverageSoloQRank().value();
          numberOfAccountRanked++;
        } catch (NoValueRankException e) {
          //Nothing to do, we ignore unranked account
        }
      }
    }

    if(totRankValue <= 0 || !config.getInfopanelRankedOption().isOptionActivated()) {
      stringMessage.append("**" + team.team_name + "**\n \n");
    }else {
      FullTier fulltier = new FullTier(totRankValue / numberOfAccountRanked);
      stringMessage.append("**" + team.team_name + "** (" + LanguageManager.getText(server.serv_language, "rankedAvg") + " : " 
          + fulltier.toStringWithoutLp(server.serv_language) + ")\n \n");
    }


    for(TreatedPlayer playerToShow : playersInTeam) { //We can order player here
      stringMessage.append(playerToShow.getInfochannelMessage());
    }

    stringMessage.append(" \n");
  }

  public static AtomicLong getNbrServerSefreshedLast2Minutes() {
    return nbrServerRefreshedLast2Minutes;
  }

}
