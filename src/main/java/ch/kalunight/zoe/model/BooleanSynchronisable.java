package ch.kalunight.zoe.model;

public class BooleanSynchronisable {

  private boolean value;
  
  public BooleanSynchronisable(boolean value) {
    this.value = value;
  }

  public synchronized boolean isValue() {
    return value;
  }

  public synchronized void setValue(boolean value) {
    this.value = value;
  }
}
