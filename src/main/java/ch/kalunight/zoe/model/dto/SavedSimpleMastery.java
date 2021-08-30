package ch.kalunight.zoe.model.dto;

import java.time.LocalDateTime;
import java.util.Date;

import ch.kalunight.zoe.util.MongodbDateUtil;
import no.stelar7.api.r4j.pojo.lol.championmastery.ChampionMastery;

public class SavedSimpleMastery {

  private ZoePlatform platform;
  
  private String summonerId;
  
  private int championId;
  
  private long championPoints;
  
  private int championLevel;
  
  private Date retrieveDate;
  
  public SavedSimpleMastery() {}
  
  public SavedSimpleMastery(ChampionMastery championMastery, String summonerId, ZoePlatform platform) {
    this.platform = platform;
    this.summonerId = summonerId;
    this.championId = championMastery.getChampionId();
    this.championPoints = championMastery.getChampionPoints();
    this.championLevel = championMastery.getChampionLevel();
    
    this.retrieveDate = MongodbDateUtil.toDate(LocalDateTime.now());
  }

  public int getChampionId() {
    return championId;
  }

  public long getChampionPoints() {
    return championPoints;
  }
  
  public int getChampionLevel() {
    return championLevel;
  }

  public void setChampionId(int championId) {
    this.championId = championId;
  }

  public void setChampionPoints(long championPoints) {
    this.championPoints = championPoints;
  }

  public void setChampionLevel(int championLevel) {
    this.championLevel = championLevel;
  }

  public ZoePlatform getPlatform() {
    return platform;
  }

  public void setPlatform(ZoePlatform platform) {
    this.platform = platform;
  }

  public String getSummonerId() {
    return summonerId;
  }

  public void setSummonerId(String summonerId) {
    this.summonerId = summonerId;
  }

  public Date getRetrieveDate() {
    return retrieveDate;
  }

  public void setRetrieveDate(Date retrieveDate) {
    this.retrieveDate = retrieveDate;
  }
}