package ch.kalunight.zoe.util.cachedRiotApi;

import net.rithms.riot.constant.Platform;

public final class SummonerKey {
	private final Platform platform;
	private final String summonerId;
	
	public SummonerKey(Platform platform, String summonerId) {
		this.platform = platform;
		this.summonerId = summonerId;
	}
	
	public boolean Equals(Object obj) {
		if(!(obj instanceof SummonerKey)) {
			return false;
		}
		
		SummonerKey key = (SummonerKey)obj;

		return key.platform == platform && key.summonerId.equals(summonerId);
	}
}
