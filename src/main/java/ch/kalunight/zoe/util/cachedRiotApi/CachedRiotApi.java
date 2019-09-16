package ch.kalunight.zoe.util.cachedRiotApi;

import java.util.HashMap;

import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

/**
 * This class is a wrapper for riotApi which will cache some data to reduces requests to riot servers
 */
public class CachedRiotApi {
	private final RiotApi riotApi;
	
	private final HashMap<SummonerKey, Summoner> cachedSummoners = new HashMap<>();
	
	public CachedRiotApi(RiotApi riotApi) {
		this.riotApi = riotApi;
	}
	
	public Summoner getSummoner(Platform platform, String summonerId, CallPriority priority) throws RiotApiException {
		SummonerKey key = new SummonerKey(platform, summonerId);
		
		Summoner summoner = cachedSummoners.get(key);
		
		if(summoner == null) {
			summoner = riotApi.getSummoner(platform, summonerId, CallPriority.NORMAL);
			
			cachedSummoners.put(key, summoner);
		}
		
		return summoner;
	}
}
