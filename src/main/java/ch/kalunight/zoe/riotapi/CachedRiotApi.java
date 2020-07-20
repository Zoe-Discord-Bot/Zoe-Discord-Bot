package ch.kalunight.zoe.riotapi;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.dto.DTO;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiAsync;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.match.dto.MatchTimeline;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.request.ratelimit.RateLimitException;
import net.rithms.riot.constant.Platform;

/**
 * This class is a wrapper for riotApi which will cache some data to reduces requests to riot servers
 */
public class CachedRiotApi {

  public static final boolean CACHE_ENABLE = true;

  public static final int RIOT_API_HUGE_LIMIT = 15000;
  public static final Duration RIOT_API_HUGE_TIME = Duration.ofMinutes(10);

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

  static {
    for(Platform platform : Platform.values()) {
      callByEndpoints.put(platform, new AtomicInteger(0));
    }
  }

  private static void increaseCallCountForGivenRegion(Platform platform) {
    callByEndpoints.get(platform).incrementAndGet();
  }

  public CachedRiotApi(RiotApi riotApi) {
    this.riotApi = riotApi;
  }

  public synchronized void cleanCache() throws SQLException {
    CacheManager.cleanMatchCache();
  }

  public MatchTimeline getMatchTimeLine(Platform platform, long matchId) throws RiotApiException {

    MatchTimeline match = riotApi.getTimelineByMatchId(platform, matchId);

    increaseCallCountForGivenRegion(platform);

    return match;
  }
  
  public MatchTimeline getMatchTimelineWithRateLimit(Platform server, long gameId) {
    MatchTimeline match = null;
    boolean needToRetry;
    do {
      
      needToRetry = true;
      try {
        increaseCallCountForGivenRegion(server);
        match = riotApi.getTimelineByMatchId(server, gameId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          logger.info("Waiting rate limit ({} sec) to retry", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }
      }
    }while(needToRetry);
    
    return match;
  }
  
  public Match getMatch(Platform platform, long matchId) throws RiotApiException {

    Match match = riotApi.getMatch(platform, matchId);

    apiMatchRequestCount.incrementAndGet();

    allMatchRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    return match;
  }

  public DTO.MatchCache getCachedMatch(Platform platform, long gameID) throws SQLException {
    return CacheManager.getMatch(platform, gameID);
  }

  public MatchList getMatchListByAccountId(Platform platform, String accountId, Set<Integer> champion, Set<Integer> queue,
      Set<Integer> season, long beginTime, long endTime, int beginIndex, int endIndex) throws RiotApiException {
    MatchList matchList = riotApi.getMatchListByAccountId(platform, accountId, champion, queue, season, beginTime, endTime, beginIndex, endIndex);

    matchListRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    return matchList;
  }
  
  public MatchList getMatchListByAccountIdWithRateLimit(Platform platform, String accountId, Set<Integer> champion, Set<Integer> queue,
      Set<Integer> season, long beginTime, long endTime, int beginIndex, int endIndex) throws RiotApiException {
    
    MatchList matchList = null;
    boolean needToRetry;
    do {
      matchListRequestCount.incrementAndGet();
      increaseCallCountForGivenRegion(platform);
      
      needToRetry = true;
      try {
        matchList = riotApi.getMatchListByAccountId(platform, accountId, champion, queue, season, beginTime, endTime, beginIndex, endIndex);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          logger.info("Waiting rate limit ({} sec) to retry in getMatchList", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit in getSummoner !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }
      }
    }while(needToRetry);

    return matchList;
  }

  public Summoner getSummoner(Platform platform, String summonerId) throws RiotApiException {
    Summoner summoner = riotApi.getSummoner(platform, summonerId);

    summonerRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    return summoner;
  }

  public Summoner getSummonerWithRateLimit(Platform platform, String summonerId) throws RiotApiException {
    Summoner summoner = null;
    boolean needToRetry;
    do {
      summonerRequestCount.incrementAndGet();
      increaseCallCountForGivenRegion(platform);
      
      needToRetry = true;
      try {
        summoner = riotApi.getSummoner(platform, summonerId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          logger.info("Waiting rate limit ({} sec) to retry in getSummoner", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit in getSummoner !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }
      }
    }while(needToRetry);
    
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
  
  public Set<LeagueEntry> getLeagueEntriesBySummonerIdWithRateLimit(Platform platform, String summonerId) throws RiotApiException {
    Set<LeagueEntry> leagueEntries = null;
    boolean needToRetry;
    
    do {
      leagueEntryRequestCount.incrementAndGet();
      increaseCallCountForGivenRegion(platform);
      
      needToRetry = true;
      try {
        leagueEntries = riotApi.getLeagueEntriesBySummonerId(platform, summonerId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          logger.info("Waiting rate limit ({} sec) to retry when getting the rank", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return new HashSet<>();
        }
      }
    }while(needToRetry);
    
    return leagueEntries;
  }

  public CurrentGameInfo getActiveGameBySummoner(Platform platform, String summonerId) throws RiotApiException {
    CurrentGameInfo gameInfo = riotApi.getActiveGameBySummoner(platform, summonerId);

    currentGameInfoRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    return gameInfo;
  }
  
  public CurrentGameInfo getActiveGameBySummonerWithRateLimit(Platform platform, 
      String summonerId) throws RiotApiException {
    CurrentGameInfo gameInfo = riotApi.getActiveGameBySummoner(platform, summonerId);

    boolean needToRetry;
    do {
      currentGameInfoRequestCount.incrementAndGet();
      increaseCallCountForGivenRegion(platform);
      
      needToRetry = true;
      try {
        gameInfo = riotApi.getActiveGameBySummoner(platform, summonerId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          logger.info("Waiting rate limit ({} sec) to retry when getting current match", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }
      }
    }while(needToRetry);
    
    return gameInfo;
  }
  
  public String getValidationCode(Platform platform, String summonerId) throws RiotApiException {
    return riotApi.getThirdPartyCodeBySummoner(platform, summonerId);
  }

  public ChampionMastery getChampionMasteriesBySummonerByChampion(Platform platform, String summonerId, int championId) throws RiotApiException {
    ChampionMastery mastery = riotApi.getChampionMasteriesBySummonerByChampion(platform, summonerId, championId);

    championMasteryRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    return mastery;
  }
  
  public ChampionMastery getChampionMasteriesBySummonerByChampionWithRateLimit(Platform platform, String summonerId, int championId) throws RiotApiException {
    ChampionMastery mastery = null;
    boolean needToRetry;
    do {
      championMasteryRequestCount.incrementAndGet();
      increaseCallCountForGivenRegion(platform);
      
      needToRetry = true;
      try {
        mastery = riotApi.getChampionMasteriesBySummonerByChampion(platform, summonerId, championId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          logger.info("Waiting rate limit ({} sec) to retry when getting mastery", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }
      }
    }while(needToRetry);
    
    return mastery;
  }

  public List<ChampionMastery> getChampionMasteriesBySummoner(Platform platform, String summonerId) throws RiotApiException {
    List<ChampionMastery> masteries = riotApi.getChampionMasteriesBySummoner(platform, summonerId);

    championMasteryRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    return masteries;
  }
  
  public List<ChampionMastery> getChampionMasteriesBySummonerWithRateLimit(Platform platform, String summonerId) throws RiotApiException {
    List<ChampionMastery> masterys = null;
    boolean needToRetry;
    do {
      championMasteryRequestCount.incrementAndGet();
      increaseCallCountForGivenRegion(platform);
      
      needToRetry = true;
      try {
        masterys = riotApi.getChampionMasteriesBySummoner(platform, summonerId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          logger.info("Waiting rate limit ({} sec) to retry when getting mastery", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return new ArrayList<>();
        }
      }
    }while(needToRetry);
    
    return masterys;
  }

  public Match getMatchWithRateLimit(Platform server, long gameId) {
    Match match = null;
    boolean needToRetry;
    do {
      apiMatchRequestCount.incrementAndGet();
      allMatchRequestCount.incrementAndGet();
      
      needToRetry = true;
      try {
        increaseCallCountForGivenRegion(server);
        match = riotApi.getMatch(server, gameId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          logger.info("Waiting rate limit ({} sec) to retry", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }
      }
    }while(needToRetry);
    
    return match;
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

  public void addApiCallForARegion(int nbrCalls, Platform platform) {
    callByEndpoints.get(platform).addAndGet(nbrCalls);
  }

  public boolean isApiCallPerPlatformNeedToBeReset() {
    return lastReset.isBefore(LocalDateTime.now().minus(RIOT_API_HUGE_TIME));
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
  
  public AtomicInteger getAllMatchCounter() {
    return allMatchRequestCount;
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
