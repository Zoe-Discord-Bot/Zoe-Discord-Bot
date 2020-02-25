package ch.kalunight.zoe.model.player_data;

import ch.kalunight.zoe.exception.NoValueRankException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;

public class FullTier {

  private Tier tier;
  private Rank rank;
  private int leaguePoints;

  public FullTier(LeagueEntry leagueEntry) {
    new FullTier(Tier.valueOf(leagueEntry.getTier()), Rank.valueOf(leagueEntry.getRank()), leagueEntry.getLeaguePoints());
  }
  
  public FullTier(Tier tier, Rank rank, int leaguePoints) {
    this.tier = tier;
    this.rank = rank;
    this.leaguePoints = leaguePoints;
  }

  public int value() throws NoValueRankException {
    if(tier == Tier.UNRANKED || tier == Tier.UNKNOWN) {
      throw new NoValueRankException("Impossible to get Value of FullRank with Unranked or Unknown rank or tier");
    }

    if(tier == Tier.MASTER || tier == Tier.GRANDMASTER || tier == Tier.CHALLENGER) {
      return tier.getValue() + leaguePoints;
    }
    return tier.getValue() + rank.getValue() + leaguePoints;
  }

  @Override
  public String toString() {
    if(tier == Tier.UNRANKED || tier == Tier.UNKNOWN) {
      return tier.toString();
    }

    if(tier == Tier.MASTER || tier == Tier.GRANDMASTER || tier == Tier.CHALLENGER) {
      return tier.toString() + " " + leaguePoints + " LP";
    }

    return tier.toString() + " " + rank.toString() + " " + leaguePoints + " LP";
  }
  
  public String toStringWithoutLp() {
    if(tier == Tier.UNRANKED || tier == Tier.UNKNOWN) {
      return tier.toString();
    }

    if(tier == Tier.MASTER || tier == Tier.GRANDMASTER || tier == Tier.CHALLENGER) {
      return tier.toString();
    }

    return tier.toString() + " " + rank.toString();
  }
  
  public FullTier getHeigerDivision() {
    if(tier.equals(Tier.MASTER)) {
      return new FullTier(Tier.GRANDMASTER, Rank.IV, 0);
    }else if(tier.equals(Tier.GRANDMASTER)) {
      return new FullTier(Tier.CHALLENGER, Rank.IV, 0);
    }else if(tier.equals(Tier.CHALLENGER)) {
      return new FullTier(Tier.CHALLENGER, Rank.IV, 0);
    }
    
    if(rank.equals(Rank.I)) {
      return new FullTier(Tier.getTierWithValue(tier.getValue() + 100), Rank.IV, 0);
    }else {
      return new FullTier(tier, Rank.getRankWithValue(rank.getValue() + 100), 0);
    }
  }

  public Tier getTier() {
    return tier;
  }

  public void setTier(Tier rank) {
    this.tier = rank;
  }

  public Rank getRank() {
    return rank;
  }

  public void setRank(Rank tier) {
    this.rank = tier;
  }

  public int getLeaguePoints() {
    return leaguePoints;
  }

  public void setLeaguePoints(int leaguePoints) {
    this.leaguePoints = leaguePoints;
  }

}
