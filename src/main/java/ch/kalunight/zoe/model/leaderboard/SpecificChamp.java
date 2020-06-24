package ch.kalunight.zoe.model.leaderboard;

import ch.kalunight.zoe.model.static_data.Champion;

public class SpecificChamp {
  private Champion champion;

  public SpecificChamp(Champion champion) {
    this.champion = champion;
  }
  
  public Champion getChampion() {
    return champion;
  }

  public void setChampion(Champion champion) {
    this.champion = champion;
  }
}
