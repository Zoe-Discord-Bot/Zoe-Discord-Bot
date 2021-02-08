package ch.kalunight.zoe.model.team;

import net.rithms.riot.api.endpoints.clash.constant.TeamPosition;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class AccountDataWithRole {
  
  private Summoner summoner;
  private Platform platform;
  private TeamPosition position;
  
  public AccountDataWithRole(Summoner summoner, Platform platform, TeamPosition position) {
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

  public Platform getPlatform() {
    return platform;
  }

  public void setPlatform(Platform platform) {
    this.platform = platform;
  }

  public TeamPosition getPosition() {
    return position;
  }

  public void setPosition(TeamPosition position) {
    this.position = position;
  }
  
}
