package ch.kalunight.zoe.model;

import java.util.List;

import ch.kalunight.zoe.util.GameQueueConfigIdUtil;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;

public enum GameQueueConfigId {
  SOLOQ(420, "soloq", "RANKED_SOLO_5x5", GameQueueConfigIdUtil.getAllSoloqType()),
  FLEX(440, "flex", "RANKED_FLEX_SR", GameQueueConfigIdUtil.getAllFlexType()),
  RANKED_TFT(1100, "tft", "RANKED_TFT", GameQueueConfigIdUtil.getAllTftType());
  
  private int id;
  private String nameId;
  private String queueType;
  private List<GameQueueType> gameQueueType;
  
  private GameQueueConfigId(int id, String nameId, String queueType, List<GameQueueType> gameQueueType) {
    this.id = id;
    this.nameId = nameId;
    this.queueType = queueType;
    this.gameQueueType = gameQueueType;
    ;
  }

  public int getId() {
    return id;
  }

  public String getNameId() {
    return nameId;
  }
  
  public List<GameQueueType> getGameQueueType() {
    return gameQueueType;
  }

  public static GameQueueConfigId getGameQueueIdWithQueueType(GameQueueType type) {
    for(GameQueueConfigId gameQueue : GameQueueConfigId.values()) {
      if(gameQueue.getGameQueueType().contains(type)) {
        return gameQueue;
      }
    }
    return null;
  }
}
