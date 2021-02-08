package ch.kalunight.zoe.model.dto;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;

public class SavedChampionsMastery {

  private List<SavedSimpleMastery> championMasteries;

  public SavedChampionsMastery(List<ChampionMastery> championMasteries) {
    List<SavedSimpleMastery> savedSimpleMasteries = new ArrayList<>();

    if(championMasteries != null) {
      for(ChampionMastery championMastery : championMasteries) {
        savedSimpleMasteries.add(new SavedSimpleMastery(championMastery));
      }
    }

    this.championMasteries = savedSimpleMasteries;
  }

  @Nullable
  public SavedSimpleMastery getChampionMasteryWithChampionId(int id) {
    for(SavedSimpleMastery championMastery : championMasteries) {
      if(championMastery.getChampionId() == id) {
        return championMastery;
      }
    }

    return null;
  }

  public List<SavedSimpleMastery> getChampionMasteries() {
    return championMasteries;
  }
}
