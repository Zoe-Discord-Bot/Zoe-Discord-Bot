package ch.kalunight.zoe.util.cachedRiotApi;

import net.rithms.riot.constant.Platform;

public final class MatchKey {
	private final Platform platform;
	private final long matchId;
	
	public MatchKey(Platform platform, long matchId) {
		this.platform = platform;
		this.matchId = matchId;
	}
	
	public boolean Equals(Object obj) {
		if(!(obj instanceof MatchKey)) {
			return false;
		}
		
		MatchKey key = (MatchKey)obj;

		return key.platform == platform && key.matchId == matchId;
	}
}
