package ch.kalunight.zoe.model.dto;

import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;

public class SavedSimpleMastery {

  private int championId;
  
  private long championPoint;
  
  public SavedSimpleMastery(ChampionMastery championMastery) {
    this.championId = championMastery.getChampionId();
    this.championPoint = championMastery.getChampionPoints();
  }

  public int getChampionId() {
    return championId;
  }

  public long getChampionPoint() {
    return championPoint;
  }
}
