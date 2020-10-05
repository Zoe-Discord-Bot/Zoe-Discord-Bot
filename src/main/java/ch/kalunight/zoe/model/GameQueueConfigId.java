package ch.kalunight.zoe.model;

public enum GameQueueConfigId {
  SOLOQ(420, "soloq", "RANKED_SOLO_5x5"),
  FLEX(440, "flex", "RANKED_FLEX_SR"),
  RANKED_TFT(1100, "tft", "RANKED_TFT");
  
  private int id;
  private String nameId;
  private String queueType;
  
  private GameQueueConfigId(int id, String nameId, String queueType) {
    this.id = id;
    this.nameId = nameId;
    this.queueType = queueType;
  }

  public int getId() {
    return id;
  }

  public String getNameId() {
    return nameId;
  }
  
  public String getQueueType() {
    return queueType;
  }

  public static GameQueueConfigId getGameQueueIdWithId(int id) {
    for(GameQueueConfigId gameQueue : GameQueueConfigId.values()) {
      if(gameQueue.getId() == id) {
        return gameQueue;
      }
    }
    return null;
  }
  
  public static GameQueueConfigId getGameQueueWithQueueType(String queueType) {
    for(GameQueueConfigId gameQueue : GameQueueConfigId.values()) {
      if(gameQueue.getQueueType().equals(queueType)) {
        return gameQueue;
      }
    }
    return null;
  }
  
}
