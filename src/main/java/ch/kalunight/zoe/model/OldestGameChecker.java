package ch.kalunight.zoe.model;

import java.time.LocalDateTime;

public class OldestGameChecker {

  private LocalDateTime oldestGameDateTime;

  public synchronized void updateOldestGameDateTime(LocalDateTime dateToCheck) {
    if(oldestGameDateTime == null || oldestGameDateTime.isAfter(dateToCheck)) {
      oldestGameDateTime = dateToCheck;
    }
  }
  
  public LocalDateTime getOldestGameDateTime() {
    return oldestGameDateTime;
  }
}
