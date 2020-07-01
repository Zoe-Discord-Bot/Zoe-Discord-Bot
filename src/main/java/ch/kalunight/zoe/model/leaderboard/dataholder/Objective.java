package ch.kalunight.zoe.model.leaderboard.dataholder;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.LeaderboardExtraDataHandler;
import ch.kalunight.zoe.model.leaderboard.NoSpecificDataNeededHandler;
import ch.kalunight.zoe.model.leaderboard.SpecificChampionObjectiveDataHandler;
import ch.kalunight.zoe.model.leaderboard.SpecificQueueDataHandler;

public enum Objective {
  MASTERY_POINT("leaderboardObjectiveTotalMasterPoint", 100),
  MASTERY_EVERYONE_START_FROM_0("leaderboardObjectiveMasterPointStartFrom0", 110),
  MASTERY_POINT_SPECIFIC_CHAMP("leaderboardObjectiveMasterPointSpecificChamp", 101),
  MASTERY_POINT_START_FROM_0_SPECIFIC_CHAMP("leaderboardObjectiveMasterPointSpecificChampStartFrom0", 111),
  SPECIFIC_QUEUE_RANK("leaderboardSpecificQueueRank", 200),
  BEST_OF_ALL_RANK("leaderboardBestOfAllRank", 201),
  SPECIFIC_QUEUE_RANK_PROGRESSION("leaderboardObjectiveSpecificQueueRankProgress", 210),
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
  
  public static LeaderboardExtraDataHandler getDataNeeded(Objective objective, EventWaiter waiter,
      Server server, CommandEvent event) {
    switch(objective) {
      case MASTERY_POINT_SPECIFIC_CHAMP:
      case MASTERY_POINT_START_FROM_0_SPECIFIC_CHAMP:
        return new SpecificChampionObjectiveDataHandler(objective, waiter, event, server);
      case SPECIFIC_QUEUE_RANK:
        return new SpecificQueueDataHandler(objective, waiter, event, server);
      default:
        return new NoSpecificDataNeededHandler(objective, waiter, event, server);
    }
  }
}
