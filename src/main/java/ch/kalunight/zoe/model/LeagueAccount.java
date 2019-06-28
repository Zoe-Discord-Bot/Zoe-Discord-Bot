package ch.kalunight.zoe.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.Zoe;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class LeagueAccount {

  private static final Logger logger = LoggerFactory.getLogger(LeagueAccount.class);

  private Summoner summoner;
  private Platform region;
  private CurrentGameInfo currentGameInfo;

  public LeagueAccount(Summoner summoner, Platform region) {
    this.summoner = summoner;
    this.region = region;
    this.currentGameInfo = null;
  }

  public void refreshCurrentGameInfo() {
    try { 
      currentGameInfo = Zoe.getRiotApi().getActiveGameBySummoner(region, summoner.getId());
    } catch(RiotApiException e) {
      logger.info(e.getMessage());
      currentGameInfo = null;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    LeagueAccount other = (LeagueAccount) obj;
    if (region != other.region)
      return false;
    if (summoner == null) {
      if (other.summoner != null)
        return false;
    } else if (!summoner.getId().equals(other.getSummoner().getId())) {
      return false;
    }
    return true;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((region == null) ? 0 : region.hashCode());
    result = prime * result + ((summoner == null) ? 0 : summoner.hashCode());
    return result;
  }

  public Summoner getSummoner() {
    return summoner;
  }

  public void setSummoner(Summoner summoner) {
    this.summoner = summoner;
  }

  public Platform getRegion() {
    return region;
  }

  public void setRegion(Platform region) {
    this.region = region;
  }

  public CurrentGameInfo getCurrentGameInfo() {
    return currentGameInfo;
  }

  public void setCurrentGameInfo(CurrentGameInfo currentGameInfo) {
    this.currentGameInfo = currentGameInfo;
  }
}
