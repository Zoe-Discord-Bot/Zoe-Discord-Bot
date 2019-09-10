package ch.kalunight.zoe.model.player_data;

import ch.kalunight.zoe.exception.NoValueRankException;

public class FullTier {

  private Tier tier;
  private Rank rank;
  private int leaguePoints;

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
      return tier.toString() + " (" + leaguePoints + " LP)";
    }

    return tier.toString() + " " + rank.toString() + " (" + leaguePoints + " LP)";
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
