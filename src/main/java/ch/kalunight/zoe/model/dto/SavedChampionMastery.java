package ch.kalunight.zoe.model.dto;

import java.util.List;

import javax.annotation.Nullable;

import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;

public class SavedChampionMastery {
  
  private List<ChampionMastery> championMasteries;
  
  public SavedChampionMastery(List<ChampionMastery> championMasteries) {
    this.championMasteries = championMasteries;
  }

  @Nullable
  public ChampionMastery getChampionMasteryWithChampionId(int id) {
     for(ChampionMastery championMastery : championMasteries) {
       if(championMastery.getChampionId() == id) {
         return championMastery;
       }
     }
     
     return null;
  }
  
  public List<ChampionMastery> getChampionMasteries() {
    return championMasteries;
  }
}
