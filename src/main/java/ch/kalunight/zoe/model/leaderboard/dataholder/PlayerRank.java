package ch.kalunight.zoe.model.leaderboard.dataholder;

import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO;

public class PlayerRank implements Comparable<PlayerRank>{

    private DTO.Player player;
    private long points;
    private GameQueueConfigId queue;
    
    public PlayerRank(DTO.Player player, long points, GameQueueConfigId queue) {
      this.player = player;
      this.points = points;
      this.queue = queue;
    }

    @Override
    public int compareTo(PlayerRank otherPlayer) {
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

    public GameQueueConfigId getQueue() {
      return queue;
    }
}
