package ch.kalunight.zoe.model.leaderboard.dataholder;

import java.util.Optional;

import ch.kalunight.zoe.model.GameQueueConfigId;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;

public class QueueSelected {
  
  private int queueId;
  
  public QueueSelected(GameQueueConfigId gameQueue) {
    this.queueId = gameQueue.getId();
  }

  public GameQueueType getGameQueue() {
    Optional<GameQueueType> queue = GameQueueType.getFromId(queueId);
    if(queue.isPresent()) {
      return queue.get();
    }
    return null;
  }
  
  public String getNameId() {
    GameQueueType gameQueue = getGameQueue();
    
    return GameQueueConfigId.getGameQueueWithQueueType(gameQueue.getApiName()).getNameId();
  }
  
  public int getQueueId() {
    return queueId;
  }

  public void setQueueId(int queueId) {
    this.queueId = queueId;
  }
  
}
