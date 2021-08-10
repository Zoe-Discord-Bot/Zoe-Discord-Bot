package ch.kalunight.zoe.model.player_data;

import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.translation.LanguageManager;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;

public class FullTier implements Comparable<FullTier> {

  private Tier tier;
  private Rank rank;
  private int leaguePoints;

  public FullTier(LeagueEntry leagueEntry) {
    this(Tier.valueOf(leagueEntry.getTier()), Rank.valueOf(leagueEntry.getRank()), leagueEntry.getLeaguePoints());
  }

  public FullTier(Tier tier, Rank rank, int leaguePoints) {
    this.tier = tier;
    this.rank = rank;
    this.leaguePoints = leaguePoints;
  }

  public FullTier(int value) {
    this.tier = Tier.getTierWithValueApproximate(value);
    value -= tier.getValue();
    if(!tier.equals(Tier.MASTER) && !tier.equals(Tier.GRANDMASTER) && !tier.equals(Tier.CHALLENGER)) {
      this.rank = Rank.getRankWithValueApproximate(value);
      value -= rank.getValue();
      this.leaguePoints = value;
    }else {
      this.rank = Rank.IV;
      this.leaguePoints = value;
    }
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

  public String toString(String lang) {
    if(tier == Tier.UNRANKED || tier == Tier.UNKNOWN) {
      return LanguageManager.getText(lang, tier.getTranslationTag());
    }

    if(tier == Tier.MASTER || tier == Tier.GRANDMASTER || tier == Tier.CHALLENGER) {
      return LanguageManager.getText(lang, tier.getTranslationTag()) + " " + leaguePoints + " LP";
    }

    return LanguageManager.getText(lang, tier.getTranslationTag()) + " " + rank.toString() + " " + leaguePoints + " LP";
  }

  public String toStringWithoutLp(String lang) {
    if(tier == Tier.UNRANKED || tier == Tier.UNKNOWN) {
      return LanguageManager.getText(lang, tier.getTranslationTag());
    }

    if(tier == Tier.MASTER || tier == Tier.GRANDMASTER || tier == Tier.CHALLENGER) {
      return LanguageManager.getText(lang, tier.getTranslationTag());
    }

    return LanguageManager.getText(lang, tier.getTranslationTag()) + " " + rank.toString();
  }

  public FullTier getHeigerDivision() {
    if(tier.equals(Tier.MASTER)) {
      return new FullTier(Tier.GRANDMASTER, Rank.I, 0);
    }else if(tier.equals(Tier.GRANDMASTER)) {
      return new FullTier(Tier.CHALLENGER, Rank.I, 0);
    }else if(tier.equals(Tier.CHALLENGER)) {
      return new FullTier(Tier.CHALLENGER, Rank.I, 0);
    }

    if(rank.equals(Rank.I)) {
      return new FullTier(Tier.getTierWithValue(tier.getValue() + 400), Rank.IV, 0);
    }else {
      return new FullTier(tier, Rank.getRankWithValue(rank.getValue() + 100), 0);
    }
  }
  
  @Override
  public int compareTo(FullTier otherFullTier) {
    if(otherFullTier == null) {
      throw new NullPointerException("OtherFullTier is null in FullTier.java");
    }
    
    if((rank == Rank.UNKNOWN || rank == Rank.UNRANKED) && (otherFullTier.rank != Rank.UNRANKED && otherFullTier.rank != Rank.UNKNOWN)) {
      return 1;
    }else if((otherFullTier.rank == Rank.UNKNOWN || otherFullTier.rank == Rank.UNRANKED) 
        && (rank != Rank.UNRANKED && rank != Rank.UNKNOWN)) {
      return -1;
    }
    
    if(tier.getValue() < otherFullTier.tier.getValue()) {
      return 1;
    }else if(otherFullTier.tier.getValue() < tier.getValue()) {
      return -1;
    }
    
    if(rank.getValue() < otherFullTier.rank.getValue()) {
      return 1;
    }else if(otherFullTier.rank.getValue() < rank.getValue()) {
      return -1;
    }
    
    if(leaguePoints < otherFullTier.leaguePoints) {
      return 1;
    }else if(otherFullTier.leaguePoints < leaguePoints) {
      return -1;
    }
      
    return 0;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + leaguePoints;
    result = prime * result + ((rank == null) ? 0 : rank.hashCode());
    result = prime * result + ((tier == null) ? 0 : tier.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj)
      return true;
    if(obj == null)
      return false;
    if(getClass() != obj.getClass())
      return false;
    FullTier other = (FullTier) obj;
    if(leaguePoints != other.leaguePoints)
      return false;
    if(rank != other.rank)
      return false;
    if(tier != other.tier)
      return false;
    return true;
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
