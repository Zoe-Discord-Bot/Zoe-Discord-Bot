package ch.kalunight.zoe.model.dto;

import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;

public class SavedSimpleMastery {

  private int championId;
  
  private long championPoints;
  
  private int championLevel;
  
  public SavedSimpleMastery(ChampionMastery championMastery) {
    this.championId = championMastery.getChampionId();
    this.championPoints = championMastery.getChampionPoints();
    this.championLevel = championMastery.getChampionLevel();
  }

  public int getChampionId() {
    return championId;
  }

  public long getChampionPoints() {
    return championPoints;
  }
  
  public int getChampionLevel() {
    return championLevel;
  }
}
