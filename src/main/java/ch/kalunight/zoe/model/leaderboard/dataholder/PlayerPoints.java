package ch.kalunight.zoe.model.leaderboard.dataholder;

import ch.kalunight.zoe.model.dto.DTO;

public class PlayerPoints implements Comparable<PlayerPoints> {
  
  private DTO.Player player;
  private long points;
  
  public PlayerPoints(DTO.Player player, long points) {
    this.player = player;
    this.points = points;
  }

  @Override
  public int compareTo(PlayerPoints otherPlayer) {
    if(points < otherPlayer.points) {
      return 1;
    }else if(points > otherPlayer.points) {
      return -1;
    }
    
    return 0;
  }

  public DTO.Player getPlayer() {
    return player;
  }
  
  public long getPoints() {
    return points;
  }

}
