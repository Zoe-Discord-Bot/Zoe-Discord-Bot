package ch.kalunight.zoe.model;

import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;

public class GameAccessDataServerSpecific {

  private String gameId;
  private LeagueShard platform;
  private Long guildId;

  public GameAccessDataServerSpecific(String gameId, LeagueShard platform, Long guildId) {
    this.gameId = gameId;
    this.platform = platform;
    this.guildId = guildId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((gameId == null) ? 0 : gameId.hashCode());
    result = prime * result + ((platform == null) ? 0 : platform.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    
    if (getClass() != obj.getClass()) {
      return false;
    }
    
    GameAccessDataServerSpecific other = (GameAccessDataServerSpecific) obj;
    if (gameId == null) {
      if (other.gameId != null) {
        return false;
      }
    } else if (!gameId.equals(other.gameId)) {
      return false;
    }
    
    if (platform != other.platform) {
      return false;
    }
    
    if (!guildId.equals(other.guildId)) {
      return false;
    }
    
    return true;
  }

  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

  public LeagueShard getPlatform() {
    return platform;
  }

  public void setPlatform(LeagueShard platform) {
    this.platform = platform;
  }

  public Long getGuildId() {
    return guildId;
  }

  public void setGuildId(Long guildId) {
    this.guildId = guildId;
  }

}
