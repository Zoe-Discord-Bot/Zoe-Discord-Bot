package ch.kalunight.zoe.model.leaderboard;

import java.util.List;
import java.util.ArrayList;

public enum Objective {
  MASTERY_POINT("leaderboardObjectiveMasterPoint", 100),
  MASTERY_EVERYONE_START_FROM_0("leaderboardObjectiveMasterPointStartFrom0", 110),
  MASTERY_POINT_SPECIFIC_CHAMP("leaderboardObjectiveMasterPointSpecificChamp", 101),
  MASTERY_POINT_START_FROM_0_SPECIFIC_CHAMP("leaderboardObjectiveMasterPointSpecificChampStartFrom0", 111),
  RANK("leaderboardObjectiveRank", 200),
  RANK_PROGRESSION("leaderboardObjectiveRankProgress", 210),
  AVERAGE_KDA("leaderboardObjectiveAverageKDA", 300);
  
  private String translationId;
  private int id;

  private Objective(String translationId, int id) {
    this.translationId = translationId;
    this.id = id;
  }

  public String getTranslationId() {
    return translationId;
  }
  
  public int getId() {
    return id;
  }
  
  public static Objective getObjectiveWithId(int id) {
    for(Objective objective : Objective.values()) {
      if(objective.getId() == id) {
        return objective;
      }
    }
    
    return null;
  }
  
  public static List<String> getDataNeeded(Objective objective){
    List<String> dataNeeded = new ArrayList<>();
    switch(objective) {
      case MASTERY_POINT_SPECIFIC_CHAMP:
      case MASTERY_POINT_START_FROM_0_SPECIFIC_CHAMP:
        dataNeeded.add("leaderboardDataNeededSpecificChamp");
        return dataNeeded;
      default:
        return dataNeeded;
    }
  }
}
