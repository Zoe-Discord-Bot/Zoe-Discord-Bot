package ch.kalunight.zoe.riotapi;

public enum DataType {
  MATCH("match");
  
  private String folderName;
  
  private DataType(String folderName) {
    this.folderName = folderName;
  }
  
  @Override
  public String toString() {
    return folderName;
  }
}
