package ch.kalunight.zoe.service.infochannel;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.RankHistoryChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.repositories.CurrentGameInfoRepository;
import ch.kalunight.zoe.repositories.LastRankRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.riotapi.CachedRiotApi;
import ch.kalunight.zoe.service.RankedChannelRefresher;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.FullTierUtil;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.tft_league.dto.TFTLeagueEntry;
import net.rithms.riot.constant.Platform;

public class ThreathPlayer implements Runnable {

  protected static final List<Player> playersInWork = Collections.synchronizedList(new ArrayList<>());

  protected static final Logger logger = LoggerFactory.getLogger(ThreathPlayer.class);

  protected static final CachedRiotApi riotApi = Zoe.getRiotApi();

  private Player player;

  private Server server;

  private ServerConfiguration serverConfig;

  private StringBuilder infochannelMessage;

  private RankHistoryChannel rankChannel;

  private FullTier rank;

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

    private ThreathPlayer getEnclosingInstance() {
      return ThreathPlayer.this;
    }
  }

  public ThreathPlayer(Server server, Player player, ServerConfiguration configuration) {
    this.player = player;
    this.server = server;
    this.infochannelMessage = new StringBuilder();
    this.serverConfig = configuration;
    playersInWork.add(player);
  }


  @Override
  public void run() {
    try {
      refreshPlayer();
      generateText();
    }catch(SQLException e) {
      logger.error("Unexpected SQLException when threathing text", e);
    }catch(Exception e) {
      logger.error("Unexpected exception when threathing text", e);
    }finally {
      playersInWork.remove(player);
    }
  }


  private void refreshPlayer() throws SQLException {
    List<LeagueAccount> leaguesAccount = LeagueAccountRepository.getLeaguesAccountsWithPlayerID(server.serv_guildId, player.player_id);

    for(LeagueAccount leagueAccount : leaguesAccount) {
      DTO.CurrentGameInfo currentGameDb = CurrentGameInfoRepository.getCurrentGameWithLeagueAccountID(leagueAccount.leagueAccount_id);

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
        manageNewGame(leagueAccount, currentGame);
      }else if(currentGameDb != null && currentGame != null) {
        if(currentGame.getGameId() != currentGameDb.currentgame_currentgame.getGameId()) {
          manageChangeGame(leagueAccount, currentGameDb, currentGame);
        }
      }else if(currentGameDb != null && currentGame == null) {
        manageDeleteGame(leagueAccount, currentGameDb, currentGame);
      }
    }
  }


  private void manageDeleteGame(LeagueAccount leagueAccount, DTO.CurrentGameInfo currentGameDb, CurrentGameInfo currentGame)
      throws SQLException {
    
    boolean refreshRankChannelIsNeeded = false;
    
    synchronized(server) {
      if(checkIfTheGameAlreadyExist(leagueAccount, currentGame)) {
        CurrentGameInfoRepository.deleteCurrentGame(currentGameDb, server);
        refreshRankChannelIsNeeded = true;
      }
    }
    
    if(refreshRankChannelIsNeeded) {
      searchForRefreshRankChannel(currentGameDb);
    }
  }


  private void manageChangeGame(LeagueAccount leagueAccount, DTO.CurrentGameInfo currentGameDb, CurrentGameInfo currentGame)
      throws SQLException {
    
    boolean refreshRankChannelIsNeeded = false;

    synchronized(server) {
      if(checkIfTheGameAlreadyExist(leagueAccount, currentGame)) {
        LeagueAccountRepository.updateAccountCurrentGameWithAccountId(leagueAccount.leagueAccount_id, currentGame.getGameId());
      }else {
        CurrentGameInfoRepository.deleteCurrentGame(currentGameDb, server);

        refreshRankChannelIsNeeded = true;

        CurrentGameInfoRepository.createCurrentGame(currentGame, leagueAccount);
      }
    }
    
    if(refreshRankChannelIsNeeded) {
      searchForRefreshRankChannel(currentGameDb);
    }
  }

  private void manageNewGame(LeagueAccount leagueAccount, CurrentGameInfo currentGame) throws SQLException {
    synchronized(server) {
      if(checkIfTheGameAlreadyExist(leagueAccount, currentGame)) {
        LeagueAccountRepository.updateAccountCurrentGameWithAccountId(leagueAccount.leagueAccount_id, currentGame.getGameId());
      }else {
        CurrentGameInfoRepository.createCurrentGame(currentGame, leagueAccount);
      }
    }
  }

  private boolean checkIfTheGameAlreadyExist(LeagueAccount leagueAccount, CurrentGameInfo currentGame) throws SQLException {
    return null != CurrentGameInfoRepository.getCurrentGameWithServerAndGameId(leagueAccount.leagueAccount_server, Long.toString(currentGame.getGameId()));
  }

  private void searchForRefreshRankChannel(DTO.CurrentGameInfo currentGameDb) throws SQLException {

    List<LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithCurrentGameId(currentGameDb.currentgame_id);

    for(LeagueAccount leagueAccount : leaguesAccounts) {
      if(currentGameDb.currentgame_currentgame.getParticipantByParticipantId(leagueAccount.leagueAccount_summonerId) != null) {
        Player playerToUpdate = PlayerRepository.getPlayerByLeagueAccountAndGuild(server.serv_guildId,
            leagueAccount.leagueAccount_summonerId, leagueAccount.leagueAccount_server);
        updateRankChannelMessage(playerToUpdate, leagueAccount, currentGameDb);
      }
    }
  }

  private void updateRankChannelMessage(DTO.Player player, DTO.LeagueAccount leagueAccount, DTO.CurrentGameInfo currentGameDb)
      throws SQLException {
    CurrentGameInfo gameOfTheChange = currentGameDb.currentgame_currentgame;

    LastRank rankBeforeThisGame = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);
    if(rankBeforeThisGame == null) {
      LastRankRepository.createLastRank(leagueAccount.leagueAccount_id);
      rankBeforeThisGame = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);
    }

    if(gameOfTheChange.getGameQueueConfigId() == GameQueueConfigId.SOLOQ.getId()) {

      LeagueEntry entry = RiotRequest.getLeagueEntrySoloq(leagueAccount.leagueAccount_summonerId, leagueAccount.leagueAccount_server);
      RankedChannelRefresher rankedRefresher = 
          new RankedChannelRefresher(rankChannel, rankBeforeThisGame.lastRank_soloq, entry,
              gameOfTheChange, player, leagueAccount, server);
      ServerData.getRankedMessageGenerator().execute(rankedRefresher);

    }else if(gameOfTheChange.getGameQueueConfigId() == GameQueueConfigId.FLEX.getId()) {

      LeagueEntry entry = RiotRequest.getLeagueEntryFlex(leagueAccount.leagueAccount_summonerId, leagueAccount.leagueAccount_server);
      RankedChannelRefresher rankedRefresher = 
          new RankedChannelRefresher(rankChannel, rankBeforeThisGame.lastRank_flex, entry,
              gameOfTheChange, player, leagueAccount, server);
      ServerData.getRankedMessageGenerator().execute(rankedRefresher);
    }

  }


  private void generateText() throws SQLException {
    List<DTO.LeagueAccount> leagueAccounts = player.leagueAccounts;

    List<DTO.LeagueAccount> accountsInGame = new ArrayList<>();
    List<DTO.LeagueAccount> accountNotInGame = new ArrayList<>();

    for(DTO.LeagueAccount leagueAccount : leagueAccounts) {
      DTO.CurrentGameInfo currentGameInfo = CurrentGameInfoRepository.getCurrentGameWithLeagueAccountID(leagueAccount.leagueAccount_id);
      if(currentGameInfo != null) {
        accountsInGame.add(leagueAccount);
      }else {
        accountNotInGame.add(leagueAccount);
      }
    }

    if(accountsInGame.isEmpty()) {

      if(serverConfig.getInfopanelRankedOption().isOptionActivated()) {

        if(accountNotInGame.size() == 1) {

          LeagueAccount leagueAccount = accountNotInGame.get(0);

          getTextInformationPanelRankOption(infochannelMessage, player, leagueAccount, false);
        }else {

          infochannelMessage.append(String.format(LanguageManager.getText(server.serv_language, "infoPanelRankedTitleMultipleAccount"), player.getUser().getAsMention()) + "\n");

          for(DTO.LeagueAccount leagueAccount : accountNotInGame) {

            getTextInformationPanelRankOption(infochannelMessage, player, leagueAccount, true);
          }
        }

      } else {
        notInGameWithoutRankInfo(infochannelMessage, player);
      }
    }else if (accountsInGame.size() == 1) {
      infochannelMessage.append(player.getUser().getAsMention() + " : " 
          + InfoPanelRefresherUtil.getCurrentGameInfoStringForOneAccount(accountsInGame.get(0), server.serv_language) + "\n");
    }else {
      infochannelMessage.append(player.getUser().getAsMention() + " : " 
          + LanguageManager.getText(server.serv_language, "informationPanelMultipleAccountInGame") + "\n"
          + InfoPanelRefresherUtil.getCurrentGameInfoStringForMultipleAccounts(accountsInGame, server.serv_language));
    }
  }

  private void getTextInformationPanelRankOption(final StringBuilder stringMessage, DTO.Player player,
      DTO.LeagueAccount leagueAccount, boolean mutlipleAccount) throws SQLException {
    LastRank lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);

    LeagueEntry soloq = null;
    LeagueEntry flex = null;
    TFTLeagueEntry tft = null;

    if(lastRank == null) {
      Set<LeagueEntry> leaguesEntry;
      Set<TFTLeagueEntry> tftLeagueEntry;
      try {
        leaguesEntry = Zoe.getRiotApi().getLeagueEntriesBySummonerIdWithRateLimit(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);
        tftLeagueEntry = Zoe.getRiotApi().getTFTLeagueEntryWithRateLimit(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);
      } catch (RiotApiException e) {
        notInGameWithoutRankInfo(stringMessage, player);
        return;
      }

      for(LeagueEntry leaguePosition : leaguesEntry) {
        if(leaguePosition.getQueueType().equals(GameQueueConfigId.SOLOQ.getNameId())) {
          soloq = leaguePosition;
        }else if(leaguePosition.getQueueType().equals(GameQueueConfigId.FLEX.getNameId())) {
          flex = leaguePosition;
        }
      }

      for(TFTLeagueEntry tftRank : tftLeagueEntry) {
        if(tftRank.getQueueType().equals(GameQueueConfigId.TFT.getNameId())) {
          tft = tftRank;
        }
      }


      if((soloq != null || flex != null || tft != null) && LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id) == null) {
        LastRankRepository.createLastRank(leagueAccount.leagueAccount_id);


        if(soloq != null) {
          LastRankRepository.updateLastRankSoloqWithLeagueAccountId(soloq, leagueAccount.leagueAccount_id);

        }

        if(flex != null) {
          LastRankRepository.updateLastRankFlexWithLeagueAccountId(flex, leagueAccount.leagueAccount_id);
        }

        if(tft != null) {
          LastRankRepository.updateLastRankTftWithLeagueAccountId(tft, leagueAccount.leagueAccount_id);
        }
        lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);
      }

    }

    if(lastRank == null) {
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
      accountString = leagueAccount.leagueAccount_name;
    }else {
      baseText = "infoPanelRankedTextOneAccount";
      accountString = player.getUser().getAsMention();
    }

    List<LastRankQueue> lastRanksByQueue = new ArrayList<>();

    if(lastRank.lastRank_soloqLastRefresh != null) {
      lastRanksByQueue.add(new LastRankQueue(lastRank.lastRank_soloq, lastRank.lastRank_soloqSecond, lastRank.lastRank_soloqLastRefresh, GameQueueConfigId.SOLOQ));
    }

    if(lastRank.lastRank_flexLastRefresh != null) {
      lastRanksByQueue.add(new LastRankQueue(lastRank.lastRank_flex, lastRank.lastRank_flexSecond, lastRank.lastRank_flexLastRefresh, GameQueueConfigId.FLEX));
    }

    if(lastRank.lastRank_tftLastRefresh != null) {
      lastRanksByQueue.add(new LastRankQueue(lastRank.lastRank_tft, lastRank.lastRank_tftSecond, lastRank.lastRank_tftLastRefresh, GameQueueConfigId.TFT));
    }


    LastRankQueue rankQueueToShow = null;
    for(LastRankQueue lastRankToCheck : lastRanksByQueue) {
      if(rankQueueToShow == null || rankQueueToShow.lastRefresh.isAfter(lastRankToCheck.lastRefresh)) {
        rankQueueToShow = lastRankToCheck;
      }
    }


    if(rankQueueToShow != null) {

      FullTier lastRankFullTier = new FullTier(rankQueueToShow.leagueEntry);

      stringMessage.append(getDetailledRank(rankQueueToShow.leagueEntry, rankQueueToShow.leagueEntrySecond, lastRankFullTier, accountString, baseText, rankQueueToShow.queue));

    } else {

      if(soloq != null) {
        FullTier soloQTier = new FullTier(soloq);

        stringMessage.append(getDetailledRank(soloq, null, soloQTier, accountString, baseText, GameQueueConfigId.SOLOQ));
        LastRankRepository.updateLastRankSoloqWithLeagueAccountId(soloq, leagueAccount.leagueAccount_id);

      }else {

        if(tft != null) {

          FullTier tftFullTier = new FullTier(tft);

          stringMessage.append(getDetailledRank(tft, null, tftFullTier, accountString, baseText, GameQueueConfigId.TFT));
          LastRankRepository.updateLastRankSoloqWithLeagueAccountId(tft, leagueAccount.leagueAccount_id);

        }else {
          if(mutlipleAccount) {
            notInGameUnranked(stringMessage, leagueAccount);
          }else {
            notInGameWithoutRankInfo(stringMessage, player);
          }
        }
      }
    }
  }

  private String getDetailledRank(LeagueEntry leagueEntryFirst, LeagueEntry leagueEntrySecond, FullTier tier, String accountString, String baseText, GameQueueConfigId rankedQueue) {
    return String.format(LanguageManager.getText(server.serv_language, baseText), accountString, 
        Ressources.getTierEmote().get(tier.getTier()).getUsableEmote() + " " + tier.toString(server.serv_language),
        FullTierUtil.getTierRankTextDifference(leagueEntrySecond, leagueEntryFirst, server.serv_language, rankedQueue)
        + " / " + LanguageManager.getText(server.serv_language, rankedQueue.getNameId())) + "\n";
  }

  private void notInGameWithoutRankInfo(final StringBuilder stringMessage, DTO.Player player) {
    stringMessage.append(player.getUser().getAsMention() + " : " 
        + LanguageManager.getText(server.serv_language, "informationPanelNotInGame") + " \n");
  }

  private void notInGameUnranked(final StringBuilder stringMessage, DTO.LeagueAccount leagueAccount) {
    stringMessage.append("- " + leagueAccount.leagueAccount_name + " : " 
        + LanguageManager.getText(server.serv_language, "unranked") + " \n");
  }

  public String getInfochannelMessage() {
    return infochannelMessage.toString();
  }

  public static void awaitAll(List<Player> playersToWait) {

    boolean needToWait;

    do {
      needToWait = false;
      for(Player playerToWait : playersToWait) {
        if(playersInWork.contains(playerToWait)) {
          needToWait = true;
          break;
        }
      }

      if(needToWait) {
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
          logger.error("Thread as been interupt when waiting Match Worker !", e);
          Thread.currentThread().interrupt();
        }
      }
    }while(needToWait);
  }

}
