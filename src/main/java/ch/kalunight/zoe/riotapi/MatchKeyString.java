package ch.kalunight.zoe.riotapi;

import ch.kalunight.zoe.model.dto.ZoePlatform;

public class MatchKeyString {
  
  private final ZoePlatform platform;
  private final String matchId;
  
  public MatchKeyString(ZoePlatform platform, String matchId) {
    this.platform = platform;
    this.matchId = matchId;
    
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((matchId == null) ? 0 : matchId.hashCode());
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
    MatchKeyString other = (MatchKeyString) obj;
    if (matchId == null) {
      if (other.matchId != null)
        return false;
    } else if (!matchId.equals(other.matchId))
      return false;
    if (platform != other.platform)
      return false;
    return true;
  }

  public ZoePlatform getPlatform() {
    return platform;
  }

  public String getMatchId() {
    return matchId;
  }

}
