package ch.kalunight.zoe.riotapi;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiAsync;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

/**
 * This class is a wrapper for riotApi which will cache some data to reduces requests to riot servers
 */
public class CachedRiotApi {

  public static final boolean CACHE_ENABLE = false;
  
  public static final int RIOT_API_HUGE_LIMIT = 15000;
  public static final Duration RIOT_API_HUGE_TIME = Duration.ofMinutes(10);
  
  public static final int RIOT_API_LOW_LIMIT = 250;
  public static final Duration RIOT_API_LOW_TIME = Duration.ofSeconds(10);
  
  private static final Logger logger = LoggerFactory.getLogger(CachedRiotApi.class);
  
  private static LocalDateTime lastReset = LocalDateTime.now();
  
  private final RiotApi riotApi;

  private final AtomicInteger apiMatchRequestCount = new AtomicInteger(0);
  private final AtomicInteger allMatchRequestCount = new AtomicInteger(0);
  private final AtomicInteger matchListRequestCount = new AtomicInteger(0);
  private final AtomicInteger summonerRequestCount = new AtomicInteger(0);
  private final AtomicInteger leagueEntryRequestCount = new AtomicInteger(0);
  private final AtomicInteger championMasteryRequestCount = new AtomicInteger(0);
  private final AtomicInteger currentGameInfoRequestCount = new AtomicInteger(0);
  private static final Map<Platform, AtomicInteger> callByEndpoints = Collections.synchronizedMap(new EnumMap<Platform, AtomicInteger>(Platform.class));
  private static final Map<Platform, List<LocalDateTime>> shortRangeRateLimitHandler = 
      Collections.synchronizedMap(new EnumMap<Platform, List<LocalDateTime>>(Platform.class));

  static {
    for(Platform platform : Platform.values()) {
      callByEndpoints.put(platform, new AtomicInteger(0));
      shortRangeRateLimitHandler.put(platform, new ArrayList<>());
    }
  }
  
  public static void increaseCallCountForGivenRegion(Platform platform) {
    callByEndpoints.get(platform).incrementAndGet();
    shortRangeRateLimitHandler.get(platform).add(LocalDateTime.now());
  }
  
  public CachedRiotApi(RiotApi riotApi) {
    this.riotApi = riotApi;
  }

  public synchronized void cleanCache() {
    CacheManager.cleanMatchCache();
  }
  
  public Match getMatch(Platform platform, long matchId) throws RiotApiException {
    Match match = CacheManager.getMatch(platform, Long.toString(matchId));

    if(match == null) {
      match = riotApi.getMatch(platform, matchId);

      CacheManager.putMatch(platform, match);

      apiMatchRequestCount.incrementAndGet();
    }
    
    allMatchRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);
    
    return match;
  }
  
  public MatchList getMatchListByAccountId(Platform platform, String accountId, Set<Integer> champion, Set<Integer> queue,
      Set<Integer> season, long beginTime, long endTime, int beginIndex, int endIndex) throws RiotApiException {
    MatchList matchList = riotApi.getMatchListByAccountId(platform, accountId, champion, queue, season, beginTime, endTime, beginIndex, endIndex);

    matchListRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    return matchList;
  }

  public Summoner getSummoner(Platform platform, String summonerId) throws RiotApiException {
    Summoner summoner = riotApi.getSummoner(platform, summonerId);

    summonerRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);
    
    return summoner;
  }

  public Summoner getSummonerByName(Platform platform, String summonerName) throws RiotApiException {
    Summoner summoner = riotApi.getSummonerByName(platform, summonerName);

    summonerRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);
    
    return summoner;
  }

  public Summoner getSummonerByPuuid(Platform platform, String puuid) throws RiotApiException {
    Summoner summoner = riotApi.getSummonerByPuuid(platform, puuid);

    summonerRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);
    
    return summoner;
  }

  public Set<LeagueEntry> getLeagueEntriesBySummonerId(Platform platform, String summonerId) throws RiotApiException {
    Set<LeagueEntry> leagueEntries = riotApi.getLeagueEntriesBySummonerId(platform, summonerId);

    leagueEntryRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);
    
    return leagueEntries;
  }

  public CurrentGameInfo getActiveGameBySummoner(Platform platform, String summonerId) throws RiotApiException {
    CurrentGameInfo gameInfo = riotApi.getActiveGameBySummoner(platform, summonerId);

    currentGameInfoRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);
    
    return gameInfo;
  }

  public ChampionMastery getChampionMasteriesBySummonerByChampion(Platform platform, String summonerId, int championId) throws RiotApiException {
    ChampionMastery mastery = riotApi.getChampionMasteriesBySummonerByChampion(platform, summonerId, championId);

    championMasteryRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);
    
    return mastery;
  }

  public List<ChampionMastery> getChampionMasteriesBySummoner(Platform platform, String summonerId) throws RiotApiException {
    List<ChampionMastery> masteries = riotApi.getChampionMasteriesBySummoner(platform, summonerId);

    championMasteryRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);
    
    return masteries;
  }

  public synchronized void clearCounts() {
    apiMatchRequestCount.set(0);
    allMatchRequestCount.set(0);
    matchListRequestCount.set(0);
    summonerRequestCount.set(0);
    leagueEntryRequestCount.set(0);
    championMasteryRequestCount.set(0);
    currentGameInfoRequestCount.set(0);
  }

  public int getTotalRequestCount() {
    return apiMatchRequestCount.intValue() + allMatchRequestCount.intValue() + matchListRequestCount.intValue()
        + summonerRequestCount.intValue() + leagueEntryRequestCount.intValue() + championMasteryRequestCount.intValue()
        + currentGameInfoRequestCount.intValue();
  }
  
  public RiotApiAsync getAsyncRiotApi() {
    return riotApi.getAsyncApi();
  }
  
  public void resetApiCallPerPlatform() {
    setLastReset(LocalDateTime.now());
    synchronized(callByEndpoints) {
      for(Platform platform : Platform.values()) {
        callByEndpoints.put(platform, new AtomicInteger(0));
      }
    }
  }
  
  public synchronized boolean isRequestsCanBeExecuted(int nbrRequest, Platform platform) {
    //TODO : To high detection with api call calculator. made it blocker
    
    List<LocalDateTime> callsPerTime = shortRangeRateLimitHandler.get(platform);
    
    do {
      refreshRateLimit(callsPerTime);
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        logger.error("Interuption when waiting for the rate limit !", e);
        Thread.currentThread().interrupt();
      }
    }while(callsPerTime.size() < RIOT_API_LOW_LIMIT);
    
    
    return true;
  }

  private void refreshRateLimit(List<LocalDateTime> callsPerTime) {
    List<LocalDateTime> callsToDelete = new ArrayList<>();
    
    for(LocalDateTime call : callsPerTime) {
      if(call.isBefore(LocalDateTime.now().minus(RIOT_API_LOW_TIME))){
        callsToDelete.add(call);
      }
    }
    
    for(LocalDateTime callToDelete : callsToDelete) {
      callsPerTime.remove(callToDelete);
    }
  }
  
  public void addApiCallForARegion(int nbrCalls, Platform platform) {
    callByEndpoints.get(platform).addAndGet(nbrCalls);
  }
  
  public boolean isApiCallPerPlatformNeedToBeReset() {
    return lastReset.isBefore(LocalDateTime.now().plus(RIOT_API_HUGE_TIME));
  }
  
  public int getApiCallPerPlatform(Platform platform) {
    return callByEndpoints.get(platform).intValue();
  }
  
  public int getApiCallRemainingPerRegion(Platform platform) {
    int remainingCall = RIOT_API_HUGE_LIMIT - callByEndpoints.get(platform).intValue();
    if(remainingCall < 0) {
      return 0;
    }
    return remainingCall;
  }

  public int getApiMatchRequestCount() {
    return apiMatchRequestCount.intValue();
  }

  public int getAllMatchRequestCount() {
    return allMatchRequestCount.intValue();
  }

  public int getMatchListRequestCount() {
    return matchListRequestCount.intValue();
  }

  public int getSummonerRequestCount() {
    return summonerRequestCount.intValue();
  }

  public int getLeagueEntryRequestCount() {
    return leagueEntryRequestCount.intValue();
  }

  public int getChampionMasteryRequestCount() {
    return championMasteryRequestCount.intValue();
  }

  public int getCurrentGameInfoRequestCount() {
    return currentGameInfoRequestCount.intValue();
  }

  public static void setLastReset(LocalDateTime lastReset) {
    CachedRiotApi.lastReset = lastReset;
  }
}
