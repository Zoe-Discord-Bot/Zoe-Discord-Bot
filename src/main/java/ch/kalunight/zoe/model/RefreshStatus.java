package ch.kalunight.zoe.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RefreshStatus {

  private AtomicInteger refreshRateInMinute;
  
  private AtomicBoolean smartModEnable;
  
  private LocalDateTime smartModEnd;
  
  public RefreshStatus(int lastRefreshInMinute, boolean smartMode) {
    this.refreshRateInMinute = new AtomicInteger(lastRefreshInMinute);
    this.smartModEnable = new AtomicBoolean(smartMode);
    this.smartModEnd = null;
  }
  
  public LocalDateTime getSmartModEnd() {
    return smartModEnd;
  }

  public void setSmartModEnd(LocalDateTime smartModEnd) {
    this.smartModEnd = smartModEnd;
  }

  public AtomicInteger getRefresRatehInMinute() {
    return refreshRateInMinute;
  }

  public AtomicBoolean isSmartModeEnable() {
    return smartModEnable;
  }

}
