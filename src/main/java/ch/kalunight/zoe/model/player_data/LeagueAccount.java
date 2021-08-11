package ch.kalunight.zoe.model.player_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class LeagueAccount {

  private static final Logger logger = LoggerFactory.getLogger(LeagueAccount.class);

  private Summoner summoner;
  private LeagueShard region;
  private SpectatorGameInfo currentGameInfo;

  public LeagueAccount(Summoner summoner, LeagueShard region) {
    this.summoner = summoner;
    this.region = region;
    this.currentGameInfo = null;
  }

  public void refreshCurrentGameInfo() {
    try {
      currentGameInfo = summoner.getCurrentGame();
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

  public Summoner getSummoner() {
    return summoner;
  }

  public void setSummoner(Summoner summoner) {
    this.summoner = summoner;
  }

  public LeagueShard getRegion() {
    return region;
  }

  public void setRegion(LeagueShard region) {
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
    return "LeagueAccount [summonerName=" + summoner.getName() + ", region=" + region.getRealmValue() + "]";
  }
}
