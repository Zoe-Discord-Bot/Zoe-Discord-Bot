package ch.kalunight.zoe.model;

public enum GameQueueConfigId {
  SOLOQ(420, "soloq"),
  FLEX(440, "flex");
  
  private int id;
  private String nameId;
  
  private GameQueueConfigId(int id, String nameId) {
    this.id = id;
    this.nameId = nameId;
  }

  public int getId() {
    return id;
  }

  public String getNameId() {
    return nameId;
  }
  
  public static GameQueueConfigId getGameQueueIdWithId(int id) {
    for(GameQueueConfigId gameQueue : GameQueueConfigId.values()) {
      if(gameQueue.getId() == id) {
        return gameQueue;
      }
    }
    return null;
  }
  
}
