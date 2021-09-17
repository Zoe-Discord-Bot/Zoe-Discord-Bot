package ch.kalunight.zoe.model.leaderboard.dataholder;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.player_data.FullTier;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;

public class PlayerRank implements Comparable<PlayerRank>{

    private DTO.Player player;
    private FullTier fullTier;
    private GameQueueType queue;
    
    public PlayerRank(DTO.Player player, FullTier fullTier, GameQueueType queue) {
      this.player = player;
      this.fullTier = fullTier;
      this.queue = queue;
    }

    @Override
    public int compareTo(PlayerRank otherPlayer) {
      return fullTier.compareTo(otherPlayer.fullTier);
    }

    public DTO.Player getPlayer() {
      return player;
    }
    
    public FullTier getFullTier() {
      return fullTier;
    }

    public GameQueueType getQueue() {
      return queue;
    }
}
