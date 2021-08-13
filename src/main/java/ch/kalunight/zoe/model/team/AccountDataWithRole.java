package ch.kalunight.zoe.model.team;

import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPosition;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class AccountDataWithRole {
  
  private Summoner summoner;
  private LeagueShard platform;
  private ClashPosition position;
  
  public AccountDataWithRole(Summoner summoner, LeagueShard platform, ClashPosition position) {
    this.summoner = summoner;
    this.platform = platform;
    this.position = position;
  }

  public Summoner getSummoner() {
    return summoner;
  }

  public void setSummoner(Summoner summoner) {
    this.summoner = summoner;
  }

  public LeagueShard getPlatform() {
    return platform;
  }

  public void setPlatform(LeagueShard platform) {
    this.platform = platform;
  }

  public ClashPosition getPosition() {
    return position;
  }

  public void setPosition(ClashPosition position) {
    this.position = position;
  }
  
}
