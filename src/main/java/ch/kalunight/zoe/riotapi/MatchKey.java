package ch.kalunight.zoe.riotapi;

import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;

public final class MatchKey {
  private final LeagueShard platform;
  private final long matchId;

  public MatchKey(LeagueShard platform, long matchId) {
    this.platform = platform;
    this.matchId = matchId;
    
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (matchId ^ (matchId >>> 32));
    result = prime * result + ((platform == null) ? 0 : platform.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MatchKey other = (MatchKey) obj;
    if (matchId != other.matchId)
      return false;
    if (platform != other.platform)
      return false;
    return true;
  }

}
