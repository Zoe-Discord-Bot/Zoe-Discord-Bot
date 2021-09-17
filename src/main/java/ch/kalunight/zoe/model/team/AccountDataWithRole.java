package ch.kalunight.zoe.model.team;

import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPosition;

public class AccountDataWithRole {
  
  private SavedSummoner summoner;
  private ZoePlatform platform;
  private ClashPosition position;
  
  public AccountDataWithRole(SavedSummoner summoner, ZoePlatform platform, ClashPosition position) {
    this.summoner = summoner;
    this.platform = platform;
    this.position = position;
  }

  public SavedSummoner getSummoner() {
    return summoner;
  }

  public void setSummoner(SavedSummoner summoner) {
    this.summoner = summoner;
  }

  public ZoePlatform getPlatform() {
    return platform;
  }

  public void setPlatform(ZoePlatform platform) {
    this.platform = platform;
  }

  public ClashPosition getPosition() {
    return position;
  }

  public void setPosition(ClashPosition position) {
    this.position = position;
  }
  
}
