package ch.kalunight.zoe.model.dto;

import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class SavedSummoner {

  private ZoePlatform platform;
  private String summonerId;
  private String accountId;
  private String puuid;
  private String name;
  private int level;
  
  public SavedSummoner() {}
  
  public SavedSummoner(Summoner summoner, ZoePlatform platform) {
    this.platform = platform;
    this.summonerId = summoner.getSummonerId();
    this.accountId = summoner.getAccountId();
    this.puuid = summoner.getPUUID();
    this.name = summoner.getName();
    this.level = summoner.getSummonerLevel();
  }

  public ZoePlatform getPlatform() {
    return platform;
  }
  
  public String getAccountId() {
    return accountId;
  }

  public String getName() {
    return name;
  }

  public int getLevel() {
    return level;
  }

  public String getSummonerId() {
    return summonerId;
  }

  public String getPuuid() {
    return puuid;
  }

  public void setPlatform(ZoePlatform platform) {
    this.platform = platform;
  }

  public void setSummonerId(String summonerId) {
    this.summonerId = summonerId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public void setPuuid(String puuid) {
    this.puuid = puuid;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setLevel(int level) {
    this.level = level;
  }
  
}
