package ch.kalunight.zoe.model.player_data;

public enum Tier {
  UNKNOWN("Inconnu", -1), UNRANKED("Unranked", -1), IRON("Iron", 1000), BRONZE("Bronze", 1400), SILVER("Silver", 1800), GOLD("Gold",
      2200), PLATINUM("Platinum",
          2600), DIAMOND("Diamond", 3000), MASTER("Master", 3400), GRANDMASTER("Grand Master", 3400), CHALLENGER("Challenger", 3400);

  private String name;
  private int value;

  Tier(String name, int value) {
    this.name = name;
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  @Override
  public String toString() {
    return name;
  }
}
