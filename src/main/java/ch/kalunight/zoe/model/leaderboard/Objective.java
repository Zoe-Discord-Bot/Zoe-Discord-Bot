package ch.kalunight.zoe.model.leaderboard;

public enum Objective {
  MASTERY_POINT("leaderboardObjectiveMasterPoint"),
  MASTERY_EVERYONE_START_FROM_0("leaderboardObjectiveMasterPointStartFrom0"),
  MASTERY_POINT_SPECIFIC_CHAMP("leaderboardObjectiveMasterPointSpecificChamp"),
  MASTERY_POINT_START_FROM_0_SPECIFIC_CHAMP("leaderboardObjectiveMasterPointSpecificChampStartFrom0"),
  RANK("leaderboardObjectiveRank"),
  RANK_PROGRESSION("leaderboardObjectiveRankProgress"),
  AVERAGE_KDA("leaderboardObjectiveAverageKDA");
  
  private String translationId;
  
  private Objective(String translationId) {
    this.translationId = translationId;
  }

  public String getTranslationId() {
    return translationId;
  }
}
