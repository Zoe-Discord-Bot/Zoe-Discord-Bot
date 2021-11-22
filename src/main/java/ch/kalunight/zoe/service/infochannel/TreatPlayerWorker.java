package ch.kalunight.zoe.service.infochannel;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.GameAccessDataServerSpecific;
import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.RankChannelFilterOption.RankChannelFilter;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.RankHistoryChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.static_data.TFTMatchWithId;
import ch.kalunight.zoe.repositories.CurrentGameInfoRepository;
import ch.kalunight.zoe.repositories.LastRankRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.TeamRepository;
import ch.kalunight.zoe.service.rankchannel.RankedChannelLoLRefresher;
import ch.kalunight.zoe.service.rankchannel.RankedChannelTFTRefresher;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.FullTierUtil;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import ch.kalunight.zoe.util.LastRankUtil;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.TFTMatchUtil;
import ch.kalunight.zoe.util.TreatedPlayer;
import ch.kalunight.zoe.util.ZoeUserRankManagementUtil;
import net.dv8tion.jda.api.JDA;
import no.stelar7.api.r4j.basic.exceptions.APIHTTPErrorReason;
import no.stelar7.api.r4j.basic.exceptions.APINoValidResponseException;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorParticipant;

public class TreatPlayerWorker implements Runnable {

  protected static final List<TreatPlayerWorker> playersInWork = Collections.synchronizedList(new ArrayList<>());

  protected static final Logger logger = LoggerFactory.getLogger(TreatPlayerWorker.class);

  private Player player;

  private List<LeagueAccount> leaguesAccounts;

  private JDA jda;

  private DTO.Team team;

  private Server server;

  private ServerConfiguration serverConfig;

  private StringBuilder infochannelMessage;

  private RankHistoryChannel rankChannel;

  private List<FullTier> soloqRank = new ArrayList<>();

  private Map<DTO.CurrentGameInfo, LeagueAccount> gamesToDelete = Collections.synchronizedMap(new HashMap<>());

  private Map<SpectatorGameInfo, LeagueAccount> gamesToCreate = Collections.synchronizedMap(new HashMap<>());

  private List<RankedChannelLoLRefresher> rankChannelsToProcess = Collections.synchronizedList(new ArrayList<>());

  private TreatedPlayer treatedPlayer = null;

  private boolean forceRefreshCache;

  private class LastRankQueue {
    public LeagueEntry leagueEntry;
    public LeagueEntry leagueEntrySecond;
    public LocalDateTime lastRefresh;
    public GameQueueConfigId queue;

    public LastRankQueue(LeagueEntry leagueEntry, LeagueEntry leagueEntrySecond, LocalDateTime lastRefresh, GameQueueConfigId gameQueueConfigId) {
      this.leagueEntry = leagueEntry;
      this.leagueEntrySecond = leagueEntrySecond;
      this.lastRefresh = lastRefresh;
      this.queue = gameQueueConfigId;
    }
  }

  public TreatPlayerWorker(Server server, Player player, List<LeagueAccount> leaguesAccounts,
      RankHistoryChannel rankChannel,  ServerConfiguration configuration, boolean forceRefreshCache, JDA jda) {
    this.player = player;
    this.leaguesAccounts = leaguesAccounts;
    this.server = server;
    this.rankChannel = rankChannel;
    this.infochannelMessage = new StringBuilder();
    this.serverConfig = configuration;
    this.forceRefreshCache = forceRefreshCache;
    this.jda = jda;
    playersInWork.add(this);
  }

  @Override
  public void run() {
    try {
      Map<LeagueAccount, SpectatorGameInfo> accountsInGame = Collections.synchronizedMap(new HashMap<>());
      List<LeagueAccount> accountNotInGame = new ArrayList<>();
      Map<LeagueAccount, String> accountsWithErrors = Collections.synchronizedMap(new HashMap<>());

      cleanUnreachableSummoner();

      refreshPlayer(accountsInGame, accountNotInGame, accountsWithErrors);
      generateText(accountsInGame, accountNotInGame, accountsWithErrors);
      createOutputObject();
    }catch(SQLException e) {
      logger.error("Unexpected SQLException when threathing text. Server ID : {}", server.serv_guildId, e);
    }catch(Exception e) {
      logger.error("Unexpected exception when threathing text. Server ID : {}", server.serv_guildId, e);
    }finally {
      playersInWork.remove(this);
    }
  }


  private void cleanUnreachableSummoner() {

    Iterator<LeagueAccount> leagueAccountsIterator = leaguesAccounts.iterator();

    while(leagueAccountsIterator.hasNext()) {
      LeagueAccount leagueAccount = leagueAccountsIterator.next();

      SavedSummoner summoner;
      try {
        summoner = leagueAccount.getSummoner(forceRefreshCache);
        if(summoner == null) {
          leagueAccountsIterator.remove();
        }
      }catch(APIResponseException e) {
        logger.warn("Error while the getting summoner, will not be showed on the infopannel this time. Error Code : {}", e.getReason().getCode(), e);
        leagueAccountsIterator.remove();
      }
    }
  }

  private void createOutputObject() {
    treatedPlayer = new TreatedPlayer(player, team, infochannelMessage.toString(), soloqRank, gamesToDelete, gamesToCreate, rankChannelsToProcess);
  }

  private void refreshPlayer(Map<LeagueAccount, SpectatorGameInfo> accountsInGame, List<LeagueAccount> accountNotInGame, Map<LeagueAccount, String> accountsWithErrors) throws SQLException {
    team = TeamRepository.getTeamByPlayerAndGuild(server.serv_guildId, player.player_discordId);

    for(LeagueAccount leagueAccount : leaguesAccounts) {
      try {
        LastRank lastRank = getLastRank(leagueAccount);

        if(lastRank != null) {
          refreshLoL(leagueAccount, lastRank, accountsInGame, accountNotInGame, accountsWithErrors);
          refreshTFT(leagueAccount, lastRank);

          if(forceRefreshCache) {
            forceRefreshAllRank(leagueAccount, lastRank);
          }

        }else {
          logger.warn("Error while refreshing a player ! last rank == null. Guild Id {}", server.serv_guildId);
        }

      }catch(APINoValidResponseException e) {
        logger.info("Riot servers are having trouble ! (503 errors)");

        accountsWithErrors.put(leagueAccount, LanguageManager.getText(server.getLanguage(), "leagueAccountDataUnavailableError503"));
      }
    }
  }


  private void forceRefreshAllRank(LeagueAccount leagueAccount, LastRank lastRank) throws SQLException {
    List<LeagueEntry> leagueEntries;
    try {
      leagueEntries = Zoe.getRiotApi().getLeagueEntryBySummonerId(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);

      LastRankUtil.updateLoLLastRankIfRankDifference(lastRank, leagueEntries);
    } catch(APIResponseException e) {
      logger.info("Error while refreshing rank in updateLoLLastRank. Server Id : {}", server.serv_guildId, e);
    }

    List<LeagueEntry> tftLeagueEntries = Zoe.getRiotApi().getTFTLeagueEntryConvertedByTFTSummonerId(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_tftSummonerId);

    LastRankUtil.updateTFTLastRank(lastRank, tftLeagueEntries);
  }


  private LastRank getLastRank(LeagueAccount leagueAccount) throws SQLException {
    LastRank lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);
    if(lastRank == null) {
      LastRankRepository.createLastRank(leagueAccount.leagueAccount_id);
      lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);
    }
    return lastRank;
  }


  private void refreshTFT(LeagueAccount leagueAccount, LastRank lastRank) throws SQLException {
    List<TFTMatchWithId> matchs = TFTMatchUtil.getTFTRankedMatchsSinceTheLastMessage(leagueAccount, lastRank);

    if(!matchs.isEmpty()) {
      List<LeagueEntry> tftLeagueEntries = Zoe.getRiotApi().getTFTLeagueEntryConvertedByTFTSummonerId(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_tftSummonerId);

      boolean rankUpdated = LastRankUtil.updateTFTLastRank(lastRank, tftLeagueEntries);

      if(rankChannel != null && rankUpdated) {

        if(serverConfig.getRankchannelFilterOption().getRankChannelFilter() == RankChannelFilter.LOL_ONLY) {
          return;
        }

        RankedChannelTFTRefresher tftRankedChannelRefresher = new RankedChannelTFTRefresher(rankChannel,
            lastRank.getLastRankTftSecond(), lastRank.getLastRankTft(), player, leagueAccount, server, matchs.get(0).getMatch(), jda, forceRefreshCache);
        ServerThreadsManager.getRankedMessageGenerator().execute(tftRankedChannelRefresher);
      }
    }
  }

  private void refreshLoL(LeagueAccount leagueAccount, LastRank lastRank,
      Map<LeagueAccount, SpectatorGameInfo> accountsInGame, List<LeagueAccount> accountNotInGame, Map<LeagueAccount, String> accountsWithErrors) throws SQLException {
    DTO.CurrentGameInfo currentGameDb = CurrentGameInfoRepository.getCurrentGameWithLeagueAccountID(leagueAccount.leagueAccount_id);

    SpectatorGameInfo currentGame;
    try {
      currentGame = Zoe.getRiotApi().getSpectatorGameInfo(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);
      if(currentGame == null) {
        accountNotInGame.add(leagueAccount);
      }else {
        accountsInGame.put(leagueAccount, currentGame);
      }
    } catch(APIResponseException e) {
      if(e.getReason() == APIHTTPErrorReason.ERROR_404) {
        currentGame = null;
      }else {
        logger.error("Riot Api exception in refresh LoL !", e);
        return;
      }
    }

    if(currentGameDb == null && currentGame != null) {
      manageNewGame(leagueAccount, currentGame);
    }else if(currentGameDb != null && currentGame != null) {
      if(currentGame.getGameId() != currentGameDb.currentgame_currentgame.getGameId()) {
        manageChangeGame(leagueAccount, currentGameDb, currentGame, lastRank);
      }
    }else if(currentGameDb != null) {
      manageDeleteGame(leagueAccount, currentGameDb, lastRank);
    }

    if(lastRank.getLastRankSoloq() != null) {
      soloqRank.add(new FullTier(lastRank.getLastRankSoloq()));
    }
  }


  private void manageDeleteGame(LeagueAccount leagueAccount, DTO.CurrentGameInfo currentGameDb, LastRank lastRank) throws SQLException {

    gamesToDelete.put(currentGameDb, leagueAccount);

    if(updateLoLLastRankIfGivenGameIsARanked(leagueAccount, currentGameDb, lastRank)) {
      searchForRefreshRankChannel(currentGameDb, leagueAccount, lastRank);
    }
  }


  private void manageChangeGame(LeagueAccount leagueAccount, DTO.CurrentGameInfo currentGameDb, SpectatorGameInfo currentGame,
      LastRank lastRank) throws SQLException {

    gamesToDelete.put(currentGameDb, leagueAccount);
    gamesToCreate.put(currentGame, leagueAccount);

    if(updateLoLLastRankIfGivenGameIsARanked(leagueAccount, currentGameDb, lastRank)) {
      searchForRefreshRankChannel(currentGameDb, leagueAccount, lastRank);
    }
  }

  /**
   * @return true is the update has been done correctly, false otherwise.
   */
  private boolean updateLoLLastRankIfGivenGameIsARanked(LeagueAccount leagueAccount, DTO.CurrentGameInfo currentGameDone, LastRank lastRank) throws SQLException {

    SpectatorGameInfo currentGameData = currentGameDone.currentgame_currentgame;

    if(GameQueueConfigId.getGameQueueIdWithQueueType(currentGameData.getGameQueueConfig()) == GameQueueConfigId.SOLOQ 
        || GameQueueConfigId.getGameQueueIdWithQueueType(currentGameData.getGameQueueConfig()) == GameQueueConfigId.FLEX) {

      List<LeagueEntry> leagueEntries;
      try {
        leagueEntries = Zoe.getRiotApi().getLeagueEntryBySummonerId(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);
      } catch(APIResponseException e) {
        logger.info("Error while getting leagueEntries in updateLoLLastRank.");
        return false;
      }

      return LastRankUtil.updateLoLLastRank(lastRank, leagueEntries).contains(GameQueueConfigId.getGameQueueIdWithQueueType(currentGameData.getGameQueueConfig()));
    }
    return false;
  }


  private void manageNewGame(LeagueAccount leagueAccount, SpectatorGameInfo currentGame) throws SQLException {
    DTO.CurrentGameInfo currentGameDB = CurrentGameInfoRepository.getCurrentGameWithServerAndGameId(leagueAccount.leagueAccount_server, currentGame.getGameId(), server);
    if(currentGameDB != null) {
      LeagueAccountRepository.updateAccountCurrentGameWithAccountId(leagueAccount.leagueAccount_id, currentGameDB.currentgame_id);
    }else {
      gamesToCreate.put(currentGame, leagueAccount);
    }
  }

  private void searchForRefreshRankChannel(DTO.CurrentGameInfo currentGameDb, LeagueAccount leagueAccount, LastRank lastRank) throws SQLException {

    if(serverConfig.getRankchannelFilterOption().getRankChannelFilter() == RankChannelFilter.TFT_ONLY) {
      return;
    }

    SpectatorParticipant participant = null;
    for(SpectatorParticipant participantToCheck : currentGameDb.currentgame_currentgame.getParticipants()) {
      if(participantToCheck.getSummonerId().equals(leagueAccount.leagueAccount_summonerId)) {
        participant = participantToCheck;
      }
    }

    if(participant != null) {
      Player playerToUpdate = PlayerRepository.getPlayerByLeagueAccountAndGuild(server.serv_guildId,
          leagueAccount.leagueAccount_summonerId, leagueAccount.leagueAccount_server);
      updateRankChannelMessage(playerToUpdate, leagueAccount, currentGameDb, lastRank);
    }
  }

  private void updateRankChannelMessage(DTO.Player player, DTO.LeagueAccount leagueAccount, DTO.CurrentGameInfo currentGameDb,
      LastRank lastRank) {
    SpectatorGameInfo gameOfTheChange = currentGameDb.currentgame_currentgame;

    if(GameQueueConfigId.getGameQueueIdWithQueueType(gameOfTheChange.getGameQueueConfig()) == GameQueueConfigId.SOLOQ) {

      RankedChannelLoLRefresher rankedRefresher = 
          new RankedChannelLoLRefresher(rankChannel, lastRank.getLastRankSoloqSecond(), lastRank.getLastRankSoloq(),
              gameOfTheChange, player, leagueAccount, server, jda, forceRefreshCache);

      GameAccessDataServerSpecific gameAccessData = new GameAccessDataServerSpecific(currentGameDb.currentgame_gameid, currentGameDb.currentgame_server, server.serv_guildId);
      RankedChannelLoLRefresher.addMatchToTreat(gameAccessData, leagueAccount);

      rankChannelsToProcess.add(rankedRefresher);

    }else if(GameQueueConfigId.getGameQueueIdWithQueueType(gameOfTheChange.getGameQueueConfig()) == GameQueueConfigId.FLEX) {

      RankedChannelLoLRefresher rankedRefresher = 
          new RankedChannelLoLRefresher(rankChannel, lastRank.getLastRankFlexSecond(), lastRank.getLastRankFlex(),
              gameOfTheChange, player, leagueAccount, server, jda, forceRefreshCache);

      GameAccessDataServerSpecific gameAccessData = new GameAccessDataServerSpecific(currentGameDb.currentgame_gameid, currentGameDb.currentgame_server, server.serv_guildId);
      RankedChannelLoLRefresher.addMatchToTreat(gameAccessData, leagueAccount);

      rankChannelsToProcess.add(rankedRefresher);
    }
  }

  private void generateText(Map<DTO.LeagueAccount, SpectatorGameInfo> accountsWithGame, List<DTO.LeagueAccount> accountNotInGame,
      Map<DTO.LeagueAccount, String> accountsWithErrors) throws SQLException {

    if(accountsWithGame.isEmpty()) {

      if(serverConfig.getInfopanelRankedOption().isOptionActivated()) {

        if(accountNotInGame.size() + accountsWithErrors.size() == 1) {

          if(!accountNotInGame.isEmpty()) {
            LeagueAccount leagueAccount = accountNotInGame.get(0);

            getTextInformationPanelRankOption(infochannelMessage, player, leagueAccount, false);
          }else {
            Entry<LeagueAccount, String> entry = accountsWithErrors.entrySet().iterator().next();

            infochannelMessage.append(ZoeUserRankManagementUtil.getEmotesByDiscordId(player.player_discordId) + player.retrieveUser(jda).getAsMention() + " : " 
                + entry.getValue() + " \n");
          }
        }else if (accountNotInGame.size() + accountsWithErrors.size() > 1) {

          infochannelMessage.append(String.format(LanguageManager.getText(server.getLanguage(), "infoPanelRankedTitleMultipleAccount"),
              ZoeUserRankManagementUtil.getEmotesByDiscordId(player.player_discordId) + player.retrieveUser(jda).getAsMention()) + "\n");

          for(DTO.LeagueAccount leagueAccount : accountNotInGame) {
            getTextInformationPanelRankOption(infochannelMessage, player, leagueAccount, true);
          }

          Iterator<Entry<LeagueAccount, String>> allAccountsInErrors = accountsWithErrors.entrySet().iterator();

          while(allAccountsInErrors.hasNext()) {
            Entry<LeagueAccount, String> entry = allAccountsInErrors.next();

            String accountName = LanguageManager.getText(server.getLanguage(), "unknown");
            try {
              SavedSummoner summoner = entry.getKey().getSummoner(false);

              if(summoner != null) {
                accountName = summoner.getName();
              }
            } catch (Exception e1) {
              //summoner not accessible
            }

            infochannelMessage.append("- " + accountName + " : " + entry.getValue() + "\n");
          }
        }

      } else {
        notInGameWithoutRankInfo(infochannelMessage, player);
      }
    }else if (accountsWithGame.size() == 1) {
      Entry<DTO.LeagueAccount, SpectatorGameInfo> entry = accountsWithGame.entrySet().iterator().next();
      infochannelMessage.append(ZoeUserRankManagementUtil.getEmotesByDiscordId(player.player_discordId) + player.retrieveUser(jda).getAsMention() + " : " 
          + InfoPanelRefresherUtil.getCurrentGameInfoStringForOneAccount(entry.getKey(), entry.getValue(), server.getLanguage(), forceRefreshCache) + "\n");
    }else {
      infochannelMessage.append(ZoeUserRankManagementUtil.getEmotesByDiscordId(player.player_discordId) + player.retrieveUser(jda).getAsMention() + " : " 
          + LanguageManager.getText(server.getLanguage(), "informationPanelMultipleAccountInGame") + "\n"
          + InfoPanelRefresherUtil.getCurrentGameInfoStringForMultipleAccounts(accountsWithGame, server.getLanguage(), forceRefreshCache));
    }
  }

  private void getTextInformationPanelRankOption(final StringBuilder stringMessage, DTO.Player player,
      DTO.LeagueAccount leagueAccount, boolean mutlipleAccount) throws SQLException {
    LastRank lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);

    LeagueEntry soloq = lastRank.getLastRankSoloq();
    LeagueEntry flex = lastRank.getLastRankFlex();
    LeagueEntry tft = lastRank.getLastRankTft();

    if(soloq == null && flex == null && tft == null) {
      if(mutlipleAccount) {
        notInGameUnranked(stringMessage, leagueAccount);
      }else {
        notInGameWithoutRankInfo(stringMessage, player);
      }
      return;
    }

    String accountString;
    String baseText;
    if(mutlipleAccount) {
      baseText = "infoPanelRankedTextMultipleAccount";
      accountString = leagueAccount.getSummoner(forceRefreshCache).getName();
    }else {
      baseText = "infoPanelRankedTextOneAccount";
      accountString = ZoeUserRankManagementUtil.getEmotesByDiscordId(player.player_discordId) + player.retrieveUser(jda).getAsMention();
    }

    List<LastRankQueue> lastRanksByQueue = new ArrayList<>();

    if(lastRank.lastRank_soloqLastRefresh != null) {
      lastRanksByQueue.add(new LastRankQueue(lastRank.getLastRankSoloq(), lastRank.getLastRankSoloqSecond(),
          lastRank.lastRank_soloqLastRefresh, GameQueueConfigId.SOLOQ));
    }

    if(lastRank.lastRank_flexLastRefresh != null) {
      lastRanksByQueue.add(new LastRankQueue(lastRank.getLastRankFlex(), lastRank.getLastRankFlexSecond(),
          lastRank.lastRank_flexLastRefresh, GameQueueConfigId.FLEX));
    }

    if(lastRank.lastRank_tftLastRefresh != null) {
      lastRanksByQueue.add(new LastRankQueue(lastRank.getLastRankTft(), lastRank.getLastRankTftSecond(),
          lastRank.lastRank_tftLastRefresh, GameQueueConfigId.RANKED_TFT));
    }


    LastRankQueue rankQueueToShow = null;
    for(LastRankQueue lastRankToCheck : lastRanksByQueue) {
      if(rankQueueToShow == null || rankQueueToShow.lastRefresh.isBefore(lastRankToCheck.lastRefresh)) {
        rankQueueToShow = lastRankToCheck;
      }
    }


    if(rankQueueToShow != null) {

      FullTier lastRankFullTier = new FullTier(rankQueueToShow.leagueEntry);
      
      stringMessage.append(getDetailledRank(rankQueueToShow.leagueEntry, rankQueueToShow.leagueEntrySecond, lastRankFullTier, accountString, baseText, rankQueueToShow.queue));

    } else {
      if(mutlipleAccount) {
        notInGameUnranked(stringMessage, leagueAccount);
      }else {
        notInGameWithoutRankInfo(stringMessage, player);
      }
    }
  }

  private String getDetailledRank(LeagueEntry leagueEntryFirst, LeagueEntry leagueEntrySecond, FullTier tier, String accountString, String baseText, GameQueueConfigId rankedQueue) {
    return String.format(LanguageManager.getText(server.getLanguage(), baseText), accountString, 
        Ressources.getTierEmote().get(tier.getTier()).getUsableEmote() + " " + tier.toString(server.getLanguage()),
        FullTierUtil.getTierRankTextDifference(leagueEntrySecond, leagueEntryFirst, server.getLanguage(), rankedQueue)
        + " / " + LanguageManager.getText(server.getLanguage(), rankedQueue.getNameId())) + "\n";
  }

  private void notInGameWithoutRankInfo(final StringBuilder stringMessage, DTO.Player player) {
    stringMessage.append(ZoeUserRankManagementUtil.getEmotesByDiscordId(player.player_discordId) + player.retrieveUser(jda).getAsMention() + " : " 
        + LanguageManager.getText(server.getLanguage(), "informationPanelNotInGame") + " \n");
  }

  private void notInGameUnranked(final StringBuilder stringMessage, DTO.LeagueAccount leagueAccount) {
    stringMessage.append("- " + leagueAccount.getSummoner(forceRefreshCache).getName() + " : " 
        + LanguageManager.getText(server.getLanguage(), "unranked") + " \n");
  }

  public String getInfochannelMessage() {
    return infochannelMessage.toString();
  }

  public static void awaitAll(List<TreatPlayerWorker> playersToWait) {

    boolean needToWait;

    do {
      needToWait = false;
      for(TreatPlayerWorker playerToWait : playersToWait) {
        if(playersInWork.contains(playerToWait)) {
          needToWait = true;
          break;
        }
      }

      if(needToWait) {
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
          logger.error("Thread as been interupt when waiting TreatPlayer Worker !", e);
          Thread.currentThread().interrupt();
        }
      }
    }while(needToWait);
  }

  public Player getPlayer() {
    return player;
  }

  public Server getServer() {
    return server;
  }

  public TreatedPlayer getTreatedPlayer() {
    return treatedPlayer;
  }
  public List<LeagueAccount> getLeaguesAccounts() {
    return leaguesAccounts;
  }

}
