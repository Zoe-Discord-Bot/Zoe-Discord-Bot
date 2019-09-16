package ch.kalunight.zoe.util.cachedRiotApi;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
	
	private final HashMap<MatchKey, Match> matchCache = new HashMap<>();
	
	public CachedRiotApi(RiotApi riotApi) {
		this.riotApi = riotApi;
	}

	public synchronized Match getMatch(Platform platform, long matchId, CallPriority priority) throws RiotApiException {
		MatchKey key = new MatchKey(platform, matchId);
		
		Match match = matchCache.get(key);
		
		if(match == null) {
			match = riotApi.getMatch(platform, matchId, priority);
			
			matchCache.put(key, match);
		}
		
		return match;
	}
	
	public synchronized MatchList getMatchListByAccountId(Platform platform, String accountId, Set<Integer> champion, Set<Integer> queue, Set<Integer> season,
		      long beginTime, long endTime, int beginIndex, int endIndex, CallPriority priority) throws RiotApiException {
		return riotApi.getMatchListByAccountId(platform, accountId, champion, queue, season, beginTime, endTime, beginIndex, endIndex, priority);
	}
	
	public synchronized Summoner getSummoner(Platform platform, String summonerId, CallPriority priority) throws RiotApiException {
		return riotApi.getSummoner(platform, summonerId, priority);
	}

	public synchronized Summoner getSummonerByName(Platform platform, String summonerName, CallPriority priority) throws RiotApiException {
		return riotApi.getSummonerByName(platform, summonerName, priority);
	}
	
	public Summoner getSummonerByPuuid(Platform platform, String puuid, CallPriority priority) throws RiotApiException {
		return riotApi.getSummonerByPuuid(platform, puuid, priority);
	}
	
	public synchronized Set<LeagueEntry> getLeagueEntriesBySummonerId(Platform platform, String summonerId, CallPriority callPriority) throws RiotApiException {
		return riotApi.getLeagueEntriesBySummonerId(platform, summonerId, callPriority);
	}

	public synchronized ChampionMastery getChampionMasteriesBySummonerByChampion(Platform platform, String summonerId, int championId, CallPriority priority) throws RiotApiException {
	    return riotApi.getChampionMasteriesBySummonerByChampion(platform, summonerId, championId, priority);
	}
	
	public synchronized CurrentGameInfo getActiveGameBySummoner(Platform platform, String summonerId, CallPriority priority) throws RiotApiException {
		return riotApi.getActiveGameBySummoner(platform, summonerId, priority);
	}
	
	public synchronized List<ChampionMastery> getChampionMasteriesBySummoner(Platform platform, String summonerId, CallPriority priority) throws RiotApiException {
		return riotApi.getChampionMasteriesBySummoner(platform, summonerId, priority);
	}
}
