package ch.kalunight.zoe.model.dto;

import net.rithms.riot.api.endpoints.summoner.dto.Summoner;

public class SavedSummoner {

  private String accountId;
  private String name;
  private int level;
  
  public SavedSummoner(Summoner summoner) {
    this.accountId = summoner.getAccountId();
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
  
}
