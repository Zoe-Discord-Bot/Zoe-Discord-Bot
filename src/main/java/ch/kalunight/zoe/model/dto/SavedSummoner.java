package ch.kalunight.zoe.model.dto;

import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class SavedSummoner {

  private String summonerId;
  private String accountId;
  private String puuid;
  private String name;
  private int level;
  
  public SavedSummoner(Summoner summoner) {
    this.summonerId = summoner.getSummonerId();
    this.accountId = summoner.getAccountId();
    this.puuid = summoner.getPUUID();
    this.name = summoner.getName();
    this.level = summoner.getSummonerLevel();
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
  
}
