package ch.kalunight.zoe.model.leaderboard.dataholder;

import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.util.Ressources;

public class SpecificChamp {
  private int championKey;

  public SpecificChamp(Champion champion) {
    this.championKey = champion.getKey();
  }
  
  public Champion getChampion() {
    return Ressources.getChampionDataById(championKey);
  }

  public void setChampion(Champion champion) {
    this.championKey = champion.getKey();
  }
}
