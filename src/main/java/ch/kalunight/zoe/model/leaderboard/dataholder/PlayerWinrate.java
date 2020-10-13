package ch.kalunight.zoe.model.leaderboard.dataholder;

import ch.kalunight.zoe.model.dto.DTO;

public class PlayerWinrate implements Comparable<PlayerWinrate> {

  private DTO.Player player;
  private double winrate;
  
  public PlayerWinrate(DTO.Player player, long winrate) {
    this.player = player;
    this.winrate = winrate;
  }

  @Override
  public int compareTo(PlayerWinrate otherPlayer) {
    if(winrate < otherPlayer.winrate) {
      return 1;
    }else if(winrate > otherPlayer.winrate) {
      return -1;
    }
    
    return 0;
  }

  public DTO.Player getPlayer() {
    return player;
  }
  
  public double getWinrate() {
    return winrate;
  }
}
