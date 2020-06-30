package ch.kalunight.zoe.model.leaderboard.dataholder;

import ch.kalunight.zoe.model.dto.DTO;

public class PlayerPoints implements Comparable<PlayerPoints> {
  
  private DTO.Player player;
  private long masteryPoint;
  
  public PlayerPoints(DTO.Player player, long maxMasteryPoints) {
    this.player = player;
    this.masteryPoint = maxMasteryPoints;
  }

  @Override
  public int compareTo(PlayerPoints otherPlayer) {
    if(masteryPoint < otherPlayer.masteryPoint) {
      return 1;
    }else if(masteryPoint > otherPlayer.masteryPoint) {
      return -1;
    }
    
    return 0;
  }

  public DTO.Player getPlayer() {
    return player;
  }
  
  public long getMasteryPoint() {
    return masteryPoint;
  }

}
