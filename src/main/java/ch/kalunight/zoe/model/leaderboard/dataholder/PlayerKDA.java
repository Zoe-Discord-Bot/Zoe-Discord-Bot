package ch.kalunight.zoe.model.leaderboard.dataholder;

import ch.kalunight.zoe.model.KDAReceiver;
import ch.kalunight.zoe.model.dto.DTO.Player;

public class PlayerKDA implements Comparable<PlayerKDA> {

  private Player player;
  private KDAReceiver kdaReceiver;
  
  public PlayerKDA(Player player, KDAReceiver kdaReceiver) {
    this.player = player;
    this.kdaReceiver = kdaReceiver;
  }

  public Player getPlayer() {
    return player;
  }

  public KDAReceiver getKdaReceiver() {
    return kdaReceiver;
  }

  @Override
  public int compareTo(PlayerKDA otherPlayer) {
    if(kdaReceiver.getAverageKDA() < otherPlayer.getKdaReceiver().getAverageKDA()) {
      return 1;
    }else if(kdaReceiver.getAverageKDA() > otherPlayer.getKdaReceiver().getAverageKDA()) {
      return -1;
    }
    
    return 0;
  }
}
