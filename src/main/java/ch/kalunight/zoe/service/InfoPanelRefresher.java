package ch.kalunight.zoe.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.GameInfoCardStatus;
import ch.kalunight.zoe.model.player_data.Team;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.CurrentGameInfoRepository;
import ch.kalunight.zoe.repositories.GameInfoCardRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.repositories.ServerStatusRepository;
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
import net.rithms.riot.constant.Platform;

public class InfoPanelRefresher implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(InfoPanelRefresher.class);

  private DTO.Server server;

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

  private class CurrentGameWithRegion {
    public DTO.CurrentGameInfo currentGameInfo;
    public Platform platform;

    public CurrentGameWithRegion(DTO.CurrentGameInfo currentGameInfo, Platform platform) {
      this.currentGameInfo = currentGameInfo;
      this.platform = platform;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getEnclosingInstance().hashCode();
      result = prime * result + ((currentGameInfo == null) ? 0 : currentGameInfo.hashCode());
      result = prime * result + ((platform == null) ? 0 : platform.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj)
        return true;
      if(obj == null)
        return false;
      if(getClass() != obj.getClass())
        return false;
      CurrentGameWithRegion other = (CurrentGameWithRegion) obj;
      if(!getEnclosingInstance().equals(other.getEnclosingInstance()))
        return false;
      if(currentGameInfo == null) {
        if(other.currentGameInfo != null)
          return false;
      } else if(currentGameInfo.currentgame_currentgame.getGameId() != other.currentGameInfo.currentgame_currentgame.getGameId())
        return false;
      if(platform != other.platform)
        return false;
      return true;
    }

    private InfoPanelRefresher getEnclosingInstance() {
      return InfoPanelRefresher.this;
    }
  }

  @Override
  public void run() {
    try {

      DTO.InfoChannel infoChannelDTO = InfoChannelRepository.getInfoChannel(server.serv_guildId);
      if(infoChannelDTO != null && guild != null) {
        infochannel = guild.getTextChannelById(infoChannelDTO.infochannel_channelid);
      }

      if(infochannel != null && guild != null) {

        if(needToWait) {
          TimeUnit.SECONDS.sleep(3);
        }

        List<DTO.Player> playersDTO = PlayerRepository.getPlayers(server.serv_guildId);

        cleanOldInfoChannelMessage();
        cleanUnlinkInfoCardAndCurrentGame();
        cleanRegisteredPlayerNoLongerInGuild(playersDTO);
        refreshAllLeagueAccountCurrentGamesAndDeleteOlderInfoCard(playersDTO);
        refreshGameCardStatus();

        refreshInfoPanel(infoChannelDTO);

        if(ConfigRepository.getServerConfiguration(server.serv_guildId).getInfoCardsOption().isOptionActivated()) {
          createMissingInfoCard();
          treathGameCardWithStatus();
        }

        cleanInfoChannel();
        clearLoadingEmote();
      }else {
        InfoChannelRepository.deleteInfoChannel(server);
      }
    }catch (InsufficientPermissionException e) {
      logger.debug("Permission {} missing for infochannel in the guild {}, try to autofix the issue... (Low chance to work)",
          e.getPermission().getName(), guild.getName());
      try {
        PermissionOverride permissionOverride = infochannel
            .putPermissionOverride(guild.getMember(Zoe.getJda().getSelfUser())).complete();

        permissionOverride.getManager().grant(e.getPermission()).complete();
        logger.debug("Autofix complete !");
      }catch(Exception e1) {
        logger.debug("Autofix fail ! Error message : {} ", e1.getMessage());
      }

    } catch (SQLException e) {
      logger.error("SQL Exception when refresh the infopanel !", e);
    } catch(Exception e) {
      logger.error("The thread got a unexpected error (The channel got probably deleted when the refresh append)", e);
    } finally {
      try {
        ServerRepository.updateTimeStamp(server.serv_guildId, LocalDateTime.now());
        ServerStatusRepository.updateInTreatment(ServerStatusRepository.getServerStatus(server.serv_guildId).servstatus_id, false);
      } catch(SQLException e) {
        logger.error("SQL Exception when updating timeStamp and treatment !", e);
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
        retrieveAndRemoveMessage(gameCard.gamecard_titlemessageid);
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

        ServerData.getInfocardsGenerator().execute(
            new InfoCardsWorker(server, infochannel, accountsLinked.get(0), currentGame, gameInfoCard));
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

  private void refreshInfoPanel(DTO.InfoChannel infoChannelDTO) throws SQLException {
    ArrayList<String> infoPanels = CommandEvent.splitMessage(refreshPannel());

    List<DTO.InfoPanelMessage> infoPanelMessages = InfoChannelRepository.getInfoPanelMessages(server.serv_guildId);

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

    for(int i = 0; i < infoPanels.size(); i++) {
      DTO.InfoPanelMessage infoPanel = infoPanelMessages.get(i);
      infochannel.retrieveMessageById(infoPanel.infopanel_messageId).complete().editMessage(infoPanels.get(i)).queue();
    }
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

    List<DTO.LeagueAccount> leaguesAccountInTheGame = 
        LeagueAccountRepository.getLeaguesAccountsWithCurrentGameId(currentGame.currentgame_id);

    for(DTO.LeagueAccount leagueAccount : leaguesAccountInTheGame) {
      if(leagueAccount != null) {
        LeagueAccountRepository.updateAccountGameCardWithAccountId(leagueAccount.leagueAccount_id, gameCard.gamecard_id);
      }
    }
  }

  private void refreshAllLeagueAccountCurrentGamesAndDeleteOlderInfoCard(List<DTO.Player> playersDTO) throws SQLException {
    List<CurrentGameWithRegion> gameAlreadyAskedToRiot = new ArrayList<>();

    for(DTO.Player player : playersDTO) {
      player.leagueAccounts =
          LeagueAccountRepository.getLeaguesAccounts(server.serv_guildId, player.player_discordId);

      for(DTO.LeagueAccount leagueAccount : player.leagueAccounts) {

        boolean alreadyLoaded = false;
        for(CurrentGameWithRegion currentGameWithRegion : gameAlreadyAskedToRiot) {
          if(leagueAccount.leagueAccount_server.equals(currentGameWithRegion.platform)
              && currentGameWithRegion.currentGameInfo.currentgame_currentgame.getParticipantByParticipantId(leagueAccount.leagueAccount_summonerId) != null) {
            LeagueAccountRepository.updateAccountCurrentGameWithAccountId(leagueAccount.leagueAccount_id, 
                currentGameWithRegion.currentGameInfo.currentgame_id);
            alreadyLoaded = true;
          }
        }

        if(alreadyLoaded) {
          continue;
        }

        DTO.CurrentGameInfo currentGameDb = CurrentGameInfoRepository.getCurrentGameWithLeagueAccountID(leagueAccount.leagueAccount_id);

        CurrentGameWithRegion currentGameDbRegion = null;
        if(currentGameDb != null) {
          currentGameDbRegion = 
              new CurrentGameWithRegion(currentGameDb, leagueAccount.leagueAccount_server);
        }

        if(currentGameDb == null || !gameAlreadyAskedToRiot.contains(currentGameDbRegion)) {

          CurrentGameInfo currentGame;
          try {
            currentGame = Zoe.getRiotApi().getActiveGameBySummoner(
                leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);
          } catch(RiotApiException e) {
            if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
              currentGame = null;
            }else {
              continue;
            }
          }

          if(currentGameDb == null && currentGame != null) {
            CurrentGameInfoRepository.createCurrentGame(currentGame, leagueAccount);
          }else if(currentGameDb != null && currentGame != null) {
            if(currentGame.getGameId() == currentGameDb.currentgame_currentgame.getGameId()) {
              CurrentGameInfoRepository.updateCurrentGame(currentGame, leagueAccount);
            }else {
              CurrentGameInfoRepository.deleteCurrentGame(currentGameDb, server);
              CurrentGameInfoRepository.createCurrentGame(currentGame, leagueAccount);
            }
          }else if(currentGameDb != null && currentGame == null) {
            CurrentGameInfoRepository.deleteCurrentGame(currentGameDb, server);
          }
          if(currentGame != null) {
            DTO.CurrentGameInfo currentGameInfo = CurrentGameInfoRepository.getCurrentGameWithLeagueAccountID(leagueAccount.leagueAccount_id);
            gameAlreadyAskedToRiot.add(new CurrentGameWithRegion(currentGameInfo, leagueAccount.leagueAccount_server));
          }
        }
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
    retrieveAndRemoveMessage(gameCard.gamecard_titlemessageid);
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

  private void cleaningInfoChannel() throws SQLException {

    List<Message> messagesToCheck = infochannel.getIterableHistory().stream()
        .limit(1000)
        .filter(m-> m.getAuthor().getId().equals(Zoe.getJda().getSelfUser().getId()))
        .collect(Collectors.toList());

    List<Message> messagesToDelete = new ArrayList<>();
    List<DTO.InfoPanelMessage> infoPanels = InfoChannelRepository.getInfoPanelMessages(server.serv_guildId);
    List<Long> infoPanelsId = new ArrayList<>();
    for(DTO.InfoPanelMessage infoPanel : infoPanels) {
      infoPanelsId.add(infoPanel.infopanel_messageId);
    }

    for(Message messageToCheck : messagesToCheck) {
      if(!messageToCheck.getTimeCreated().isBefore(OffsetDateTime.now().minusHours(1)) || infoPanelsId.contains(messageToCheck.getIdLong())) {
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

  private String refreshPannel() throws SQLException {

    final List<Team> teamList = TeamRepository.getAllPlayerInTeams(server.serv_guildId, server.serv_language);
    final StringBuilder stringMessage = new StringBuilder();

    stringMessage.append("__**" + LanguageManager.getText(server.serv_language, "informationPanelTitle") + "**__\n \n");

    for(Team team : teamList) {

      if(teamList.size() != 1) {
        stringMessage.append("**" + team.getName() + "**\n \n");
      }

      List<DTO.Player> playersList = team.getPlayers();

      for(DTO.Player player : playersList) {

        List<DTO.LeagueAccount> leagueAccounts = player.leagueAccounts;

        List<DTO.LeagueAccount> accountsInGame = new ArrayList<>();
        for(DTO.LeagueAccount leagueAccount : leagueAccounts) {
          DTO.CurrentGameInfo currentGameInfo = CurrentGameInfoRepository.getCurrentGameWithLeagueAccountID(leagueAccount.leagueAccount_id);
          if(currentGameInfo != null) {
            accountsInGame.add(leagueAccount);
          }
        }

        if(accountsInGame.isEmpty()) {
          stringMessage.append(player.user.getAsMention() + " : " 
              + LanguageManager.getText(server.serv_language, "informationPanelNotInGame") + " \n");
        }else if (accountsInGame.size() == 1) {
          stringMessage.append(player.user.getAsMention() + " : " 
              + InfoPanelRefresherUtil.getCurrentGameInfoStringForOneAccount(accountsInGame.get(0), server.serv_language) + "\n");
        }else {
          stringMessage.append(player.user.getAsMention() + " : " 
              + LanguageManager.getText(server.serv_language, "informationPanelMultipleAccountInGame") + "\n"
              + InfoPanelRefresherUtil.getCurrentGameInfoStringForMultipleAccounts(accountsInGame, server.serv_language));
        }
      }
      stringMessage.append(" \n");
    }

    stringMessage.append(LanguageManager.getText(server.serv_language, "informationPanelRefreshedTime"));

    return stringMessage.toString();
  }


}
