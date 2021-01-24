package ch.kalunight.zoe.model.dangerosityreport;

import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.service.analysis.ChampionRole;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import net.rithms.riot.constant.Platform;

public class PickData implements Comparable<PickData> {
  
  private Platform platform;
  
  private SummonerCache savedSummoner;
  
  private int championId;
  
  private ChampionRole role;
  
  private List<DangerosityReport> reportsOfThePick;

  public PickData(Platform platform, SummonerCache savedSummoner, int championId, ChampionRole role, List<DangerosityReport> reports) {
    this.platform = platform;
    this.championId = championId;
    this.role = role;
    this.reportsOfThePick = reports;
    this.savedSummoner = savedSummoner;
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
      return 1;
    }else if (objectToPickData.getValueOfThePick() < getValueOfThePick()) {
      return -1;
    }
    
    return 0;
  }
  
  public String getSummonerId() {
    return savedSummoner.sumCache_summonerId;
  }
  
  public List<DangerosityReport> getDangerosityReportBySource(DangerosityReportSource source){
    List<DangerosityReport> reportsToReturn = new ArrayList<>();
    
    for(DangerosityReport report : reportsOfThePick) {
      if(report.getReportSource() == source) {
        reportsToReturn.add(report);
      }
    }
    return reportsToReturn;
  }

  public Platform getPlatform() {
    return platform;
  }

  public ChampionRole getRole() {
    return role;
  }

  public SavedSummoner getSavedSummoner() {
    return savedSummoner.getSumCacheData();
  }

  public int getChampionId() {
    return championId;
  }

  public List<DangerosityReport> getReportsOfThePick() {
    return reportsOfThePick;
  }
}