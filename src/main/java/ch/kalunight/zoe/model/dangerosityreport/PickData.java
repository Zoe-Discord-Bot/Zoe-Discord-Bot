package ch.kalunight.zoe.model.dangerosityreport;

import java.util.List;

import ch.kalunight.zoe.model.dto.SavedSummoner;
import net.rithms.riot.constant.Platform;

public class PickData implements Comparable<PickData> {

  private String summonerId;
  
  private Platform platform;
  
  private SavedSummoner savedSummoner;
  
  private int championId;
  
  private List<DangerosityReport> reportsOfThePick;

  public PickData(String summonerId, Platform platform, SavedSummoner savedSummoner, int championId, List<DangerosityReport> reports) {
    this.summonerId = summonerId;
    this.platform = platform;
    this.savedSummoner = savedSummoner;
    this.championId = championId;
    this.reportsOfThePick = reports;
  }
  
  public int getValueOfThePick() {
    int totalValue = 0;
    for(DangerosityReport report : reportsOfThePick) {
      totalValue += report.getReportValue();
    }
    
    return totalValue;
  }
  
  @Override
  public int compareTo(PickData objectToPickData) {

    if(getValueOfThePick() == objectToPickData.getValueOfThePick()) {
      return 0;
    }
    
    if(objectToPickData.getValueOfThePick() > getValueOfThePick()) {
      return -1;
    }else if (objectToPickData.getValueOfThePick() < getValueOfThePick()) {
      return 1;
    }
    
    return 0;
  }
  
  public String getSummonerId() {
    return summonerId;
  }

  public Platform getPlatform() {
    return platform;
  }

  public SavedSummoner getSavedSummoner() {
    return savedSummoner;
  }

  public int getChampionId() {
    return championId;
  }

  public List<DangerosityReport> getReportsOfThePick() {
    return reportsOfThePick;
  }

}