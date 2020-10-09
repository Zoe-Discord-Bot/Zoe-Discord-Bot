package ch.kalunight.zoe.riotapi;

public enum RateLimitType {
  APP("application"),
  METHOD("method");
  
  private String typeName;
  
  private RateLimitType(String type) {
    this.typeName = type;
  }

  public String getTypeName() {
    return typeName;
  }
  
}
