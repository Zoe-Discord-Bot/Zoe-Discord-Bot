package ch.kalunight.zoe.model;

public enum Tier {
  UNKNOWN("Inconnu", -1),
  UNRANKED("Unranked", -1),
  IRON("Fer", 1000),
  BRONZE("Bronze", 1400),
  SILVER("Argent", 1800),
  GOLD("Or", 2200),
  PLATINUM("Platine", 2600),
  DIAMOND("Diamant", 3000),
  MASTER("Maitre", 3400),
  GRANDMASTER("Grand Maitre", 3400),
  CHALLENGER("Challenger", 3400);
  
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
