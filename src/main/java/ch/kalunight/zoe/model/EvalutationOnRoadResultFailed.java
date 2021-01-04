package ch.kalunight.zoe.model;

import java.time.LocalDateTime;

public class EvalutationOnRoadResultFailed {

  private int rateFailed;
  
  private LocalDateTime timeOfTheFail;
 
  public EvalutationOnRoadResultFailed(int rateFailed, LocalDateTime timeOfTheFail) {
    this.rateFailed = rateFailed;
    this.timeOfTheFail = timeOfTheFail;
  }

  public int getRateFailed() {
    return rateFailed;
  }

  public void setRateFailed(int rateFailed) {
    this.rateFailed = rateFailed;
  }

  public LocalDateTime getTimeOfTheFail() {
    return timeOfTheFail;
  }

  public void setTimeOfTheFail(LocalDateTime timeOfTheFail) {
    this.timeOfTheFail = timeOfTheFail;
  }
  
}
