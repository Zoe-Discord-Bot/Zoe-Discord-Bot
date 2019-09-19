package ch.kalunight.zoe.riotapi;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

/**
 * This class is a wrapper for riotApi which will cache some data to reduces requests to riot servers
 */
public class CachedRiotApi {

  private final RiotApi riotApi;

  private final ConcurrentHashMap<MatchKey, Match> matchCache = new ConcurrentHashMap<>();

  private int matchRequestCount = 0;
  private int cachedMatchRequestCount = 0;
  private int matchListRequestCount = 0;
  private int summonerRequestCount = 0;
  private int leagueEntryRequestCount = 0;
  private int championMasteryRequestCount = 0;
  private int currentGameInfoRequestCount = 0;

  public CachedRiotApi(RiotApi riotApi) {
    this.riotApi = riotApi;
  }

  public synchronized void cleanCache() {
    Iterator<Entry<MatchKey, Match>> cachList = matchCache.entrySet().iterator();
    
    ArrayList<MatchKey> matchsToDelete = new ArrayList<>();
    
    while(cachList.hasNext()) {
      Entry<MatchKey, Match> matchEntry = cachList.next();
      
      Match match = matchEntry.getValue();
      
      LocalDateTime dateTimeCreationMatch = LocalDateTime.ofInstant(Instant.ofEpochMilli(match.getGameCreation()), ZoneOffset.UTC);
      
      if(LocalDateTime.now().minusDays(32).isAfter(dateTimeCreationMatch)) { //32 days to avoid premature delete due to time zone
        matchsToDelete.add(matchEntry.getKey());
      }
    }
    
    for(MatchKey matchKey : matchsToDelete) {
      matchCache.remove(matchKey);
    }
  }
  
  public synchronized Match getMatch(Platform platform, long matchId, CallPriority priority) throws RiotApiException {
    MatchKey key = new MatchKey(platform, matchId);

    Match match = matchCache.get(key);

    if(match == null) {
      match = riotApi.getMatch(platform, matchId, priority);

      matchCache.put(key, match);

      matchRequestCount++;
    }
    
    cachedMatchRequestCount++;
    
    return match;
  }
  
  public synchronized MatchList getMatchListByAccountId(Platform platform, String accountId, Set<Integer> champion, Set<Integer> queue,
      Set<Integer> season, long beginTime, long endTime, int beginIndex, int endIndex, CallPriority priority) throws RiotApiException {
    MatchList matchList = riotApi.getMatchListByAccountId(platform, accountId, champion, queue, season, beginTime, endTime, beginIndex, endIndex, priority);

    matchListRequestCount++;

    return matchList;
  }

  public synchronized Summoner getSummoner(Platform platform, String summonerId, CallPriority priority) throws RiotApiException {
    Summoner summoner = riotApi.getSummoner(platform, summonerId, priority);

    summonerRequestCount++;

    return summoner;
  }

  public synchronized Summoner getSummonerByName(Platform platform, String summonerName, CallPriority priority) throws RiotApiException {
    Summoner summoner = riotApi.getSummonerByName(platform, summonerName, priority);

    summonerRequestCount++;

    return summoner;
  }

  public Summoner getSummonerByPuuid(Platform platform, String puuid, CallPriority priority) throws RiotApiException {
    Summoner summoner = riotApi.getSummonerByPuuid(platform, puuid, priority);

    summonerRequestCount++;

    return summoner;
  }

  public synchronized Set<LeagueEntry> getLeagueEntriesBySummonerId(Platform platform, String summonerId, CallPriority callPriority) throws RiotApiException {
    Set<LeagueEntry> leagueEntries = riotApi.getLeagueEntriesBySummonerId(platform, summonerId, callPriority);

    leagueEntryRequestCount++;

    return leagueEntries;
  }

  public synchronized CurrentGameInfo getActiveGameBySummoner(Platform platform, String summonerId, CallPriority priority) throws RiotApiException {
    CurrentGameInfo gameInfo = riotApi.getActiveGameBySummoner(platform, summonerId, priority);

    currentGameInfoRequestCount++;

    return gameInfo;
  }

  public synchronized ChampionMastery getChampionMasteriesBySummonerByChampion(Platform platform, String summonerId, int championId, CallPriority priority) throws RiotApiException {
    ChampionMastery mastery = riotApi.getChampionMasteriesBySummonerByChampion(platform, summonerId, championId, priority);

    championMasteryRequestCount++;

    return mastery;
  }

  public synchronized List<ChampionMastery> getChampionMasteriesBySummoner(Platform platform, String summonerId, CallPriority priority) throws RiotApiException {
    List<ChampionMastery> masteries = riotApi.getChampionMasteriesBySummoner(platform, summonerId, priority);

    championMasteryRequestCount++;

    return masteries;
  }

  public synchronized void clearCounts() {
    matchRequestCount = 0;
    cachedMatchRequestCount = 0;

    matchListRequestCount = 0;

    summonerRequestCount = 0;

    leagueEntryRequestCount = 0;

    championMasteryRequestCount = 0;

    currentGameInfoRequestCount = 0;
  }

  public synchronized int getTotalRequestCount() {
    return matchRequestCount + cachedMatchRequestCount + matchListRequestCount
        + summonerRequestCount + leagueEntryRequestCount + championMasteryRequestCount
        + currentGameInfoRequestCount;
  }

  public synchronized  int getMatchRequestCount() {
    return matchRequestCount;
  }

  public synchronized int getCachedMatchRequestCount() {
    return cachedMatchRequestCount;
  }

  public synchronized int getMatchListRequestCount() {
    return matchListRequestCount;
  }

  public synchronized int getSummonerRequestCount() {
    return summonerRequestCount;
  }

  public synchronized int getLeagueEntryRequestCount() {
    return leagueEntryRequestCount;
  }

  public synchronized int getChampionMasteryRequestCount() {
    return championMasteryRequestCount;
  }

  public synchronized int getCurrentGameInfoRequestCount() {
    return currentGameInfoRequestCount;
  }
}
