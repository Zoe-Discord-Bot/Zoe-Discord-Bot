package ch.kalunight.zoe.model;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class KDAReceiver {
  public static final DecimalFormat DECIMAL_FORMAT_KDA = new DecimalFormat("##.#");
  
  public static final double PERFECT_KDA_VALUE = 999999999999d;
  
  public final AtomicInteger numberOfMatchs = new AtomicInteger(0);
  public final AtomicInteger kills = new AtomicInteger(0);
  public final AtomicInteger deaths = new AtomicInteger(0);
  public final AtomicInteger assists = new AtomicInteger(0);
  
  public double getAverageKDA() {
    if(numberOfMatchs.get() == 0) {
      return 0;
    }
    
    double avgKills = kills.doubleValue() / numberOfMatchs.get();
    double avgDeaths = deaths.doubleValue() / numberOfMatchs.get();
    double avgAssists = assists.doubleValue() / numberOfMatchs.get();
    
    if(avgDeaths == 0) {
      return PERFECT_KDA_VALUE;
    }
    
    return (avgKills + avgAssists) / avgDeaths;
  }
  
  public String getAverageStats() {
    if(numberOfMatchs.get() == 0) {
      return 0 + "/" + 0 + "/" + 0;
    }
    
    double avgKills = kills.doubleValue() / numberOfMatchs.get();
    double avgDeaths = deaths.doubleValue() / numberOfMatchs.get();
    double avgAssists = assists.doubleValue() / numberOfMatchs.get();
    
    return DECIMAL_FORMAT_KDA.format(avgKills) + "/" + DECIMAL_FORMAT_KDA.format(avgDeaths) + "/" + DECIMAL_FORMAT_KDA.format(avgAssists);
  }
}
