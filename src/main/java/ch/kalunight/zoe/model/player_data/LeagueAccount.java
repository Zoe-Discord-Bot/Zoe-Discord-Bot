package ch.kalunight.zoe.model.player_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;

public class LeagueAccount {

  private static final Logger logger = LoggerFactory.getLogger(LeagueAccount.class);

  private SavedSummoner summoner;
  private ZoePlatform region;
  private SpectatorGameInfo currentGameInfo;

  public LeagueAccount(SavedSummoner summoner, ZoePlatform region) {
    this.summoner = summoner;
    this.region = region;
    this.currentGameInfo = null;
  }

  public void refreshCurrentGameInfo() {
    try {
      currentGameInfo = Zoe.getRiotApi().getSpectatorGameInfo(region, summoner.getSummonerId());
    } catch(APIResponseException e) {
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
    } else if (!summoner.getSummonerId().equals(other.getSummoner().getSummonerId())) {
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

  public SavedSummoner getSummoner() {
    return summoner;
  }

  public void setSummoner(SavedSummoner summoner) {
    this.summoner = summoner;
  }

  public ZoePlatform getRegion() {
    return region;
  }

  public void setRegion(ZoePlatform region) {
    this.region = region;
  }

  public SpectatorGameInfo getCurrentGameInfo() {
    return currentGameInfo;
  }

  public void setCurrentGameInfo(SpectatorGameInfo currentGameInfo) {
    this.currentGameInfo = currentGameInfo;
  }

  @Override
  public String toString() {
    return "LeagueAccount [summonerName=" + summoner.getName() + ", region=" + region.getShowableName() + "]";
  }
}
