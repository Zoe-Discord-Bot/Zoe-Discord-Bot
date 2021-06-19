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
import ch.kalunight.zoe.model.dto.SavedChampionsMastery;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.repositories.ChampionMasteryRepository;
import ch.kalunight.zoe.repositories.ClashTournamentRepository;
import ch.kalunight.zoe.repositories.SavedMatchCacheRepository;
import ch.kalunight.zoe.repositories.SummonerCacheRepository;
import ch.kalunight.zoe.model.dto.DTO.ChampionMasteryCache;
import ch.kalunight.zoe.model.dto.DTO.ClashTournamentCache;
import ch.kalunight.zoe.model.dto.DTO.MatchCache;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiAsync;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeam;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeamMember;
import net.rithms.riot.api.endpoints.clash.dto.ClashTournament;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.match.v5.dto.MatchV5;
import net.rithms.riot.api.endpoints.match.v5.dto.MatchV5Timeline;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.endpoints.tft_league.dto.TFTLeagueEntry;
import net.rithms.riot.api.endpoints.tft_match.dto.TFTMatch;
import net.rithms.riot.api.endpoints.tft_summoner.dto.TFTSummoner;
import net.rithms.riot.api.request.ratelimit.RateLimitException;
import net.rithms.riot.constant.Platform;

/**
 * This class is a wrapper for riotApi which will cache some data to reduces requests to riot servers
 */
public class CachedRiotApi {

  public static final boolean CACHE_ENABLE = true;

  public static final int RIOT_API_HUGE_LIMIT = 15000;
  public static final int RIOT_API_TFT_HUGE_LIMIT = 30000;
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
  private static final Map<Platform, AtomicInteger> callTFTByEndpoints = Collections.synchronizedMap(new EnumMap<Platform, AtomicInteger>(Platform.class));

  static {
    for(Platform platform : Platform.values()) {
      callByEndpoints.put(platform, new AtomicInteger(0));
      callTFTByEndpoints.put(platform, new AtomicInteger(0));
    }
  }

  private static void increaseCallCountForGivenRegion(Platform platform) {
    callByEndpoints.get(platform).incrementAndGet();
  }

  private static void increaseTFTCallCountForGivenRegion(Platform platform) {
    callTFTByEndpoints.get(platform).incrementAndGet();
  }

  public CachedRiotApi(RiotApi riotApi) {
    this.riotApi = riotApi;
  }

  public synchronized void cleanCache() throws SQLException {
    CacheManager.cleanMatchCache();
  }

  public MatchV5Timeline getMatchTimeLine(Platform platform, String matchId) throws RiotApiException {

    MatchV5Timeline timeline = riotApi.getTimelineV5ByMatchId(platform, matchId);

    increaseCallCountForGivenRegion(platform);

    return timeline;
  }

  public MatchV5Timeline getMatchTimelineWithRateLimit(Platform server, String gameId) {
    MatchV5Timeline timeline = null;
    boolean needToRetry;
    do {

      needToRetry = true;
      try {
        increaseCallCountForGivenRegion(server);
        timeline = riotApi.getTimelineV5ByMatchId(server, gameId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return null;
          }
          logger.info("Waiting rate limit ({} sec) to retry", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return null;
        }
      }
    }while(needToRetry);

    return timeline;
  }

  public SavedMatch getMatch(Platform platform, String matchId) throws RiotApiException {

    try {
      MatchCache matchCache = getCachedMatch(platform, matchId);

      if(matchCache != null) {
        return matchCache.mCatch_savedMatch;
      }
    } catch (SQLException e) {
      logger.warn("Error while getting cached match", e);
    }

    MatchV5 match = riotApi.getMatchV5(platform, matchId);

    apiMatchRequestCount.incrementAndGet();

    allMatchRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    SavedMatch cachedMatch = new SavedMatch(match);

    try {
      SavedMatchCacheRepository.createMatchCache(matchId, platform, cachedMatch);
    }catch (SQLException e) {
      logger.info("Sql expection");
    }

    return cachedMatch;
  }

  public DTO.MatchCache getCachedMatch(Platform platform, String gameID) throws SQLException {
    return CacheManager.getMatch(platform, gameID);
  }

  public List<String> getMatchListByAccountId(Platform platform, String puuid, Integer startIndex, Integer count) throws RiotApiException {
    List<String> matchList = riotApi.getMatchListByPuuid(platform, puuid, startIndex, count);

    matchListRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    return matchList;
  }

  public List<String> getMatchListByPuuidWithRateLimit(Platform platform, String puuid, Integer startIndex, Integer count) throws RiotApiException {

    List<String> matchList = null;
    boolean needToRetry;
    do {
      matchListRequestCount.incrementAndGet();
      increaseCallCountForGivenRegion(platform);

      needToRetry = true;
      try {
        matchList = riotApi.getMatchListByPuuid(platform, puuid, startIndex, count);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType() == null || (e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10)) {
            return new ArrayList<>();
          }
          logger.info("Waiting rate limit ({} sec) to retry in getMatchList", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit in getMatchList !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return new ArrayList<>();
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return new ArrayList<>();
        }
      }
    }while(needToRetry);

    return matchList;
  }

  public SummonerCache getSummoner(Platform platform, String summonerId, boolean forceRefreshCache) throws RiotApiException {

    SummonerCache summonerCache = null;
    try {
      summonerCache = SummonerCacheRepository.getSummonerWithSummonerId(summonerId, platform);
    } catch (SQLException e) {
      logger.warn("Error while getting summoner cache !", e);
    }

    if((summonerCache != null && summonerCache.sumCache_lastRefresh.isAfter(LocalDateTime.now().minusHours(CacheRefreshTime.SUMMONER_CACHE_REFRESH_TIME_IN_HOURS)))
        && !forceRefreshCache) {
      return summonerCache;
    }

    try {
      Summoner summoner = riotApi.getSummoner(platform, summonerId);

      summonerRequestCount.incrementAndGet();
      increaseCallCountForGivenRegion(platform);

      if(summoner != null) {
        SavedSummoner summonerToCache = new SavedSummoner(summoner);

        try {
          if(summonerCache == null) {
            SummonerCacheRepository.createSummonerCache(summoner.getId(), platform, summonerToCache);
            summonerCache = SummonerCacheRepository.getSummonerWithSummonerId(summonerId, platform);
          }else {
            SummonerCacheRepository.updateSummonerCache(summonerToCache, summonerCache.sumCache_id);
            summonerCache.setSumCacheData(summonerToCache);
          }
        } catch (SQLException e) {
          logger.warn("Error while saving a summoner, summoner returned anyway", e);
        }

        return summonerCache;
      }
    }catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
        try {
          SummonerCacheRepository.deleteSummonerCacheWithSummonerIDAndServer(platform, summonerId);
        } catch (SQLException e1) {
          logger.warn("Impossible to delete summoner correctly, null returned anyway.");
        }
        return null;
      }else {
        throw e;
      }
    }

    return null;
  }

  public SummonerCache getSummonerWithRateLimit(Platform platform, String summonerId, boolean forceRefreshCache) throws RiotApiException {
    boolean needToRetry;
    do {
      needToRetry = true;
      try {
        SummonerCache summonerCache = null;
        try {
          summonerCache = SummonerCacheRepository.getSummonerWithSummonerId(summonerId, platform);
        } catch (SQLException e) {
          logger.warn("Error while getting summoner cache !", e);
        }

        if((summonerCache != null && summonerCache.sumCache_lastRefresh.isAfter(LocalDateTime.now().minusHours(CacheRefreshTime.SUMMONER_CACHE_REFRESH_TIME_IN_HOURS)))
            && !forceRefreshCache) {
          return summonerCache;
        }

        Summoner summoner = riotApi.getSummoner(platform, summonerId);

        needToRetry = false;

        summonerRequestCount.incrementAndGet();
        increaseCallCountForGivenRegion(platform);

        if(summoner != null) {
          SavedSummoner summonerToCache = new SavedSummoner(summoner);
          
          try {
            if(summonerCache == null) {
              SummonerCacheRepository.createSummonerCache(summoner.getId(), platform, summonerToCache);
              summonerCache = SummonerCacheRepository.getSummonerWithSummonerId(summonerId, platform);
            }else {
              SummonerCacheRepository.updateSummonerCache(summonerToCache, summonerCache.sumCache_id);
              summonerCache.setSumCacheData(summonerToCache);
            }
          } catch (SQLException e) {
            logger.warn("Error while saving a summoner, summoner returned anyway", e);
          }

          return summonerCache;
        }
      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return null;
          }
          logger.info("Waiting rate limit ({} sec) to retry in getSummoner", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit in getSummoner !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          try {
            SummonerCacheRepository.deleteSummonerCacheWithSummonerIDAndServer(platform, summonerId);
          } catch (SQLException e1) {
            logger.warn("Impossible to delete summoner correctly, null returned anyway.");
          }
          return null;
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return null;
        }
      }
    }while(needToRetry);

    return null;
  }

  public TFTSummoner getTFTSummonerByNameWithRateLimit(Platform platform, String name) throws RiotApiException {
    TFTSummoner summoner = null;
    boolean needToRetry;
    do {
      needToRetry = true;
      increaseTFTCallCountForGivenRegion(platform);
      try {
        summoner = riotApi.getTFTSummonerByName(platform, name);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return null;
          }
          logger.info("Waiting rate limit ({} sec) to retry in getTFTSummoner", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit in getSummoner !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return null;
        }
      }
    }while(needToRetry);

    return summoner;
  }

  public Summoner getSummonerByNameWithRateLimit(Platform platform, String summonerName) throws RiotApiException {
    Summoner summoner = null;
    boolean needToRetry;
    do {
      needToRetry = true;
      increaseCallCountForGivenRegion(platform);
      summonerRequestCount.incrementAndGet();
      
      try {
        summoner = riotApi.getSummonerByName(platform, summonerName);

        SavedSummoner summonerToCache = new SavedSummoner(summoner);
        
        SummonerCache currentSummonerCache = null;
        try {
          currentSummonerCache = SummonerCacheRepository.getSummonerWithSummonerId(summoner.getId(), platform);
        } catch (SQLException e) {
          logger.warn("Error while getting summoner cache !", e);
        }

        if(currentSummonerCache == null) {
          try {
            SummonerCacheRepository.createSummonerCache(summoner.getId(), platform, summonerToCache);
          } catch (SQLException e) {
            logger.warn("Error while saving a summoner, summoner returned anyway", e);
          }
        }else {
          try {
            SummonerCacheRepository.updateSummonerCache(summonerToCache, currentSummonerCache.sumCache_id);
          } catch (SQLException e) {
            logger.warn("Error while saving a summoner, summoner returned anyway", e);
          }
        }
        
        needToRetry = false;
        
      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return null;
          }
          logger.info("Waiting rate limit ({} sec) to retry in getTFTSummoner", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit in getSummoner !", e1);
          Thread.currentThread().interrupt();
        }
      }
    }while(needToRetry);

    return summoner;
  }

  public Summoner getSummonerByPuuid(Platform platform, String puuid) throws RiotApiException {
    Summoner summoner = riotApi.getSummonerByPuuid(platform, puuid);

    summonerRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    SavedSummoner summonerToCache = new SavedSummoner(summoner);

    SummonerCache summonerCache = null;
    try {
      summonerCache = SummonerCacheRepository.getSummonerWithSummonerId(summoner.getId(), platform);
    } catch (SQLException e) {
      logger.warn("Error while getting summoner cache !", e);
    }

    if(summonerCache == null) {
      try {
        SummonerCacheRepository.createSummonerCache(summoner.getId(), platform, summonerToCache);
      } catch (SQLException e) {
        logger.warn("Error while saving a summoner, summoner returned anyway", e);
      }
    }else {
      try {
        SummonerCacheRepository.updateSummonerCache(summonerToCache, summonerCache.sumCache_id);
      } catch (SQLException e) {
        logger.warn("Error while saving a summoner, summoner returned anyway", e);
      }
    }

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
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return new HashSet<>();
          }
          logger.info("Waiting rate limit ({} sec) to retry when getting the rank", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return new HashSet<>();
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
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

  public CurrentGameInfo getActiveGameBySummonerWithRateLimit(Platform platform, String summonerId) throws RiotApiException {
    CurrentGameInfo gameInfo = null;

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
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return null;
          }
          logger.info("Waiting rate limit ({} sec) to retry when getting current match", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          throw e;
        }
      }
    }while(needToRetry);

    return gameInfo;
  }

  public List<ClashTournament> getClashTournamentsWithRateLimit(Platform platform) throws SQLException {

    ClashTournamentCache clashTournamentCache = ClashTournamentRepository.getClashTournamentCache(platform);

    if(clashTournamentCache != null && !clashTournamentCache.clashTourCache_lastRefresh.isBefore(LocalDateTime.now().minusHours(CacheRefreshTime.CLASH_TOURNAMENT_REFRESH_TIME_IN_HOURS))) {
      return clashTournamentCache.clashTourCache_data;
    }else {
      refreshClashTournaments();
      clashTournamentCache = ClashTournamentRepository.getClashTournamentCache(platform);
      if(clashTournamentCache != null) {
        return clashTournamentCache.clashTourCache_data;
      }
      return new ArrayList<>();
    }
  }

  public synchronized void refreshClashTournaments() throws SQLException {

    for(Platform platform : Platform.values()) {

      boolean needToRetry;
      do {
        increaseCallCountForGivenRegion(platform);

        needToRetry = true;
        try {
          List<ClashTournament> clashTournaments = riotApi.getClashTournaments(platform);

          ClashTournamentCache tournamentCache = ClashTournamentRepository.getClashTournamentCache(platform);

          if(tournamentCache == null) {
            ClashTournamentRepository.createClashTournamentCache(platform, clashTournaments);
          }else {
            ClashTournamentRepository.updateClashTournamentCache(clashTournaments, tournamentCache.clashTourCache_id);
          }

          needToRetry = false;
        }catch(RateLimitException e) {
          try {
            if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
              break;
            }
            logger.info("Waiting rate limit ({} sec) to retry when getting clash tournament", e.getRetryAfter());
            TimeUnit.SECONDS.sleep(e.getRetryAfter());
          } catch (InterruptedException e1) {
            logger.error("Thread Interupted when waiting the rate limit !", e1);
            Thread.currentThread().interrupt();
          }
        } catch (RiotApiException e) {
          if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
            break;
          }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
            logger.warn("Bad request received from Riot Api!", e);
            break;
          }
        }
      }while(needToRetry);
    }

  }

  public ClashTournament getClashTournamentById(Platform platform, int tournamentId, boolean forceRefreshCache) throws SQLException {

    List<ClashTournament> clashTournaments = getClashTournamentsWithRateLimit(platform);

    for(ClashTournament clashTournament : clashTournaments) {
      if(clashTournament.getId() == tournamentId) {
        return clashTournament;
      }
    }

    return null;
  }

  public ClashTeam getClashTeamByTeamIdWithRateLimit(Platform platform, String teamId) throws RiotApiException {
    ClashTeam clashTeam = null;
    boolean needToRetry;

    do {
      increaseCallCountForGivenRegion(platform);

      needToRetry = true;
      try {
        clashTeam = riotApi.getClashTeamByTeamId(platform, teamId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return clashTeam;
          }
          logger.info("Waiting rate limit ({} sec) to retry when getting clash team", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return clashTeam;
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return clashTeam;
        }
      }
    }while(needToRetry);

    return clashTeam;
  }

  public List<ClashTeamMember> getClashPlayerBySummonerIdWithRateLimit(Platform platform, String summonerId) throws RiotApiException {
    List<ClashTeamMember> clashTeamMembers = new ArrayList<>();
    boolean needToRetry;

    do {
      increaseCallCountForGivenRegion(platform);

      needToRetry = true;
      try {
        clashTeamMembers = riotApi.getClashPlayersBySummoner(platform, summonerId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return clashTeamMembers;
          }
          logger.info("Waiting rate limit ({} sec) to retry when getting Clash Player", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return clashTeamMembers;
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return clashTeamMembers;
        }
      }
    }while(needToRetry);

    return clashTeamMembers;
  }

  public String getValidationCode(Platform platform, String summonerId) throws RiotApiException {
    return riotApi.getThirdPartyCodeBySummoner(platform, summonerId);
  }

  public SavedSimpleMastery getChampionMasteriesBySummonerByChampion(Platform platform, String summonerId, int championId, boolean forceRefreshCache) throws RiotApiException {

    ChampionMasteryCache championMasteryCache = null;
    if(!forceRefreshCache) {
      try {
        championMasteryCache = ChampionMasteryRepository.getChampionMasteryWithSummonerId(summonerId, platform);
      } catch (SQLException e) {
        logger.warn("Error while getting champion mastery cache !", e);
      }
    }

    if(championMasteryCache != null && championMasteryCache.champMasCache_lastRefresh.isAfter(LocalDateTime.now().minusHours(CacheRefreshTime.MASTERY_CACHE_REFRESH_TIME_IN_HOURS))) {

      SavedSimpleMastery championMastery = championMasteryCache.champMasCache_data.getChampionMasteryWithChampionId(championId);

      if(championMastery != null) {
        return championMastery;
      }else {
        return null;
      }
    }

    List<ChampionMastery> masteries = riotApi.getChampionMasteriesBySummoner(platform, summonerId);

    championMasteryRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    SavedChampionsMastery championMasteryToCache = new SavedChampionsMastery(masteries);

    SavedSimpleMastery championMasteryToReturn = championMasteryToCache.getChampionMasteryWithChampionId(championId);

    try {
      ChampionMasteryRepository.createMasteryCache(summonerId, platform, championMasteryToCache);
    } catch (SQLException e) {
      logger.warn("Error while creating a new mastery cache, result returned anyway", e);
    }

    return championMasteryToReturn;
  }

  public Set<TFTLeagueEntry> getTFTLeagueEntries(Platform platform, String summonerId) throws RiotApiException {
    increaseTFTCallCountForGivenRegion(platform);

    return riotApi.getTFTLeagueEntryBySummoner(platform, summonerId);
  }

  public Set<TFTLeagueEntry> getTFTLeagueEntriesWithRateLimit(Platform platform, String summonerId) {
    Set<TFTLeagueEntry> leagueEntries = null;
    boolean needToRetry;

    do {
      increaseTFTCallCountForGivenRegion(platform);
      needToRetry = true;
      try {
        leagueEntries = riotApi.getTFTLeagueEntryBySummoner(platform, summonerId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return new HashSet<>();
          }
          logger.info("Waiting rate limit ({} sec) to retry when getting a TFT rank", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return new HashSet<>();
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return new HashSet<>();
        }
      }
    }while(needToRetry);

    if(leagueEntries.isEmpty()) {
      return new HashSet<>();
    }

    return leagueEntries;
  }

  public List<String> getTFTMatchListWithRateLimit(Platform platform, String summonerPuuid, Integer maxMatch) {
    List<String> matchsList = null;
    boolean needToRetry;

    do {
      leagueEntryRequestCount.incrementAndGet();
      increaseTFTCallCountForGivenRegion(platform);

      needToRetry = true;
      try {
        matchsList = riotApi.getTFTMatchList(platform, summonerPuuid, maxMatch);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return new ArrayList<>();
          }
          logger.info("Waiting rate limit ({} sec) to retry when getting a TFTMatchList", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return new ArrayList<>();
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return new ArrayList<>();
        }
      }
    }while(needToRetry);

    if(matchsList.isEmpty()) {
      return new ArrayList<>();
    }

    return matchsList;
  }

  public TFTMatch getTFTMatchWithRateLimit(Platform platform, String matchId) {
    TFTMatch match = null;
    boolean needToRetry;

    do {
      leagueEntryRequestCount.incrementAndGet();
      increaseTFTCallCountForGivenRegion(platform);

      needToRetry = true;
      try {
        match = riotApi.getTFTMatch(platform, matchId);
        needToRetry = false;
      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return null;
          }
          logger.info("Waiting rate limit ({} sec) to retry when getting a TFTMatch", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return null;
        }
      }
    }while(needToRetry);

    return match;
  }

  public SavedSimpleMastery getChampionMasteriesBySummonerByChampionWithRateLimit(Platform platform, String summonerId, int championId, boolean forceRefreshCache) throws RiotApiException {
    SavedSimpleMastery championMasteryToReturn = null;
    boolean needToRetry;
    do {

      needToRetry = true;
      try {
        ChampionMasteryCache championMasteryCache = null;
        try {
          championMasteryCache = ChampionMasteryRepository.getChampionMasteryWithSummonerId(summonerId, platform);
        } catch (SQLException e) {
          logger.warn("Error while getting champion mastery cache !", e);
        }

        if((championMasteryCache != null && championMasteryCache.champMasCache_lastRefresh.isAfter(LocalDateTime.now().minusHours(CacheRefreshTime.MASTERY_CACHE_REFRESH_TIME_IN_HOURS)))
            && !forceRefreshCache) {

          SavedSimpleMastery championMastery = championMasteryCache.champMasCache_data.getChampionMasteryWithChampionId(championId);

          if(championMastery != null) {
            return championMastery;
          }else {
            return null;
          }
        }

        List<ChampionMastery> masteries = riotApi.getChampionMasteriesBySummoner(platform, summonerId);
        needToRetry = false;

        championMasteryRequestCount.incrementAndGet();
        increaseCallCountForGivenRegion(platform);

        if(masteries != null) {
          SavedChampionsMastery championMasteryToCache = new SavedChampionsMastery(masteries);

          championMasteryToReturn = championMasteryToCache.getChampionMasteryWithChampionId(championId);

          try {
            if(championMasteryCache == null) {
              ChampionMasteryRepository.createMasteryCache(summonerId, platform, championMasteryToCache);
            }else {
              ChampionMasteryRepository.updateChampionMastery(championMasteryToCache, championMasteryCache.champMasCache_id);
            }
          } catch (SQLException e) {
            logger.warn("Error while creating a new mastery cache, result returned anyway", e);
          }
        }

      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return null;
          }
          logger.info("Waiting rate limit ({} sec) to retry when getting mastery", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return null;
        }
      }
    }while(needToRetry);

    return championMasteryToReturn;
  }

  public SavedChampionsMastery getChampionMasteriesBySummoner(Platform platform, String summonerId, boolean forceRefreshCache) throws RiotApiException {
    SavedChampionsMastery championMasteries = null;
    ChampionMasteryCache championMasteryCache = null;
    try {
      championMasteryCache = ChampionMasteryRepository.getChampionMasteryWithSummonerId(summonerId, platform);
    } catch (SQLException e) {
      logger.warn("Error while getting champion mastery cache !", e);
    }

    if((championMasteryCache != null && championMasteryCache.champMasCache_lastRefresh.isAfter(LocalDateTime.now().minusHours(CacheRefreshTime.MASTERY_CACHE_REFRESH_TIME_IN_HOURS)))
        && !forceRefreshCache) {
      return championMasteryCache.champMasCache_data;
    }

    List<ChampionMastery> masteries = riotApi.getChampionMasteriesBySummoner(platform, summonerId);

    championMasteryRequestCount.incrementAndGet();
    increaseCallCountForGivenRegion(platform);

    if(masteries != null) {
      championMasteries = new SavedChampionsMastery(masteries);

      try {
        if(championMasteryCache == null) {
          ChampionMasteryRepository.createMasteryCache(summonerId, platform, championMasteries);
        }else {
          ChampionMasteryRepository.updateChampionMastery(championMasteries, championMasteryCache.champMasCache_id);
        }
      } catch (SQLException e) {
        logger.warn("Error while creating a new mastery cache, result returned anyway", e);
      }
    }

    return championMasteries;
  }

  public SavedChampionsMastery getChampionMasteriesBySummonerWithRateLimit(Platform platform, String summonerId, boolean forceRefreshCache) throws RiotApiException {
    SavedChampionsMastery masteries = null;
    boolean needToRetry;
    do {

      needToRetry = true;
      try {

        ChampionMasteryCache championMasteryCache = null;
        try {
          championMasteryCache = ChampionMasteryRepository.getChampionMasteryWithSummonerId(summonerId, platform);
          if(championMasteryCache != null) {
            masteries = championMasteryCache.champMasCache_data;
          }
        } catch (SQLException e) {
          logger.warn("Error while getting champion mastery cache !", e);
        }


        if((championMasteryCache != null && championMasteryCache.champMasCache_lastRefresh.isAfter(LocalDateTime.now().minusHours(CacheRefreshTime.MASTERY_CACHE_REFRESH_TIME_IN_HOURS)))
            && !forceRefreshCache) {
          return championMasteryCache.champMasCache_data;
        }

        List<ChampionMastery> baseDataMasteries = riotApi.getChampionMasteriesBySummoner(platform, summonerId);
        needToRetry = false;

        championMasteryRequestCount.incrementAndGet();
        increaseCallCountForGivenRegion(platform);

        if(baseDataMasteries != null) {
          masteries = new SavedChampionsMastery(baseDataMasteries);

          try {
            if(championMasteryCache == null) {
              ChampionMasteryRepository.createMasteryCache(summonerId, platform, masteries);
            }else {
              ChampionMasteryRepository.updateChampionMastery(masteries, championMasteryCache.champMasCache_id);
            }
          } catch (SQLException e) {
            logger.warn("Error while creating a new mastery cache, result returned anyway", e);
          }
        }

      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10) {
            return null;
          }
          logger.info("Waiting rate limit ({} sec) to retry when getting mastery", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return null;
        }
      }
    }while(needToRetry);

    return masteries;
  }

  public SavedMatch getMatchWithRateLimit(Platform server, String gameId) {
    SavedMatch match = null;

    try {
      MatchCache matchCache = getCachedMatch(server, gameId);

      if(matchCache != null) {
        return matchCache.mCatch_savedMatch;
      }
    } catch (SQLException e) {
      logger.warn("Error while getting cached match", e);
    }

    boolean needToRetry;
    do {
      apiMatchRequestCount.incrementAndGet();
      allMatchRequestCount.incrementAndGet();

      needToRetry = true;
      try {
        increaseCallCountForGivenRegion(server);
        MatchV5 completeMatch = riotApi.getMatchV5(server, gameId);
        if(completeMatch == null) {
          return null;
        }

        match = new SavedMatch(completeMatch);
        needToRetry = false;
        SavedMatchCacheRepository.createMatchCache(gameId, server, match);
      }catch(RateLimitException e) {
        try {
          if(e.getRateLimitType() == null || (e.getRateLimitType().equals(RateLimitType.METHOD.getTypeName()) && e.getRetryAfter() > 10)) {
            return null;
          }
          logger.info("Waiting rate limit ({} sec) to retry", e.getRetryAfter());
          TimeUnit.SECONDS.sleep(e.getRetryAfter());
        } catch (InterruptedException e1) {
          logger.error("Thread Interupted when waiting the rate limit !", e1);
          Thread.currentThread().interrupt();
        }
      } catch (RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          return null;
        }else if(e.getErrorCode() == RiotApiException.BAD_REQUEST) {
          logger.warn("Bad request received from Riot Api!", e);
          return null;
        }
      } catch (SQLException e) {
        logger.info("Error while creating match inside the cache, the result is returned anyway", e);
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
        callTFTByEndpoints.put(platform, new AtomicInteger(0));
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

  public int getApiCallRemainingPerRegionTFT(Platform platform) {
    int remainingCall = RIOT_API_TFT_HUGE_LIMIT - callTFTByEndpoints.get(platform).intValue();
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
