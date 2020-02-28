package ch.kalunight.zoe.model.player_data;

public enum Tier {
  UNKNOWN("unknown", -1),
  UNRANKED("unranked", -1),
  IRON("iron", 1000),
  BRONZE("bronze", 1400),
  SILVER("silver", 1800),
  GOLD("gold", 2200),
  PLATINUM("platinum", 2600),
  DIAMOND("diamond", 3000),
  MASTER("master", 3400),
  GRANDMASTER("grandmaster", 3400),
  CHALLENGER("challenger", 3400);

  private String translationTag;
  private int value;

  Tier(String tag, int value) {
    this.translationTag = tag;
    this.value = value;
  }

  public static Tier getTierWithValue(int value) {
    for(Tier tier : Tier.values()) {
      if(tier.getValue() == value) {
        return tier;
      }
    }
    return null;
  }
  
  public int getValue() {
    return value;
  }
  
  public String getTranslationTag() {
    return translationTag;
  }

  @Override
  public String toString() {
    return translationTag;
  }
}
