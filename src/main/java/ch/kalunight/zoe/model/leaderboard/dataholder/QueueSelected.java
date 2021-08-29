package ch.kalunight.zoe.model.leaderboard.dataholder;

import java.util.Optional;

import ch.kalunight.zoe.model.GameQueueConfigId;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;

public class QueueSelected {
  
  private int queueId;
  
  public QueueSelected(GameQueueConfigId gameQueue) {
    this.queueId = gameQueue.getId();
  }

  public GameQueueConfigId getGameQueueId() {
    Optional<GameQueueType> queue = GameQueueType.getFromId(queueId);
    if(queue.isPresent()) {
      return GameQueueConfigId.getGameQueueIdWithQueueType(queue.get());
    }
    return null;
  }
  
  public String getNameId() {
    GameQueueConfigId gameQueue = getGameQueueId();
    
    return gameQueue.getNameId();
  }
  
  public int getQueueId() {
    return queueId;
  }

  public void setQueueId(int queueId) {
    this.queueId = queueId;
  }
  
}
