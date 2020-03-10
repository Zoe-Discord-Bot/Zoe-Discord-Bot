package ch.kalunight.zoe.model;

public enum GameQueueConfigId {
  SOLOQ(420),
  FLEX(440);
  
  private int id;
  
  private GameQueueConfigId(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }
  
}
