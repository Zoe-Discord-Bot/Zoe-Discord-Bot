package ch.kalunight.zoe.model.leaderboard.dataholder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.LeaderboardExtraDataHandler;
import ch.kalunight.zoe.model.leaderboard.NoSpecificDataNeededHandler;
import ch.kalunight.zoe.model.leaderboard.SpecificChampionObjectiveDataHandler;
import ch.kalunight.zoe.model.leaderboard.SpecificQueueDataHandler;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public enum Objective {
  MASTERY_POINT("leaderboardObjectiveTotalMasterPoint", 100),
  MASTERY_POINT_SPECIFIC_CHAMP("leaderboardObjectiveMasterPointSpecificChamp", 101),
  SPECIFIC_QUEUE_RANK("leaderboardSpecificQueueRank", 200),
  BEST_OF_ALL_RANK("leaderboardBestOfAllRank", 201),
  AVERAGE_KDA("leaderboardObjectiveAverageKDA", 300),
  AVERAGE_KDA_SPECIFIC_CHAMP("leaderboardObjectiveAverageKDASpecificChamp", 301)/*,
  WINRATE("leaderboardObjectiveWinrate", 400),
  WINRATE_SPECIFIC_QUEUE("leaderboardObjectiveWinrateSpecificQueue", 401),
  WINRATE_SPECIFIC_CHAMP("leaderboardObjectiveWinrateSpecificChamp", 402)*/;

  private static final Gson gson = new GsonBuilder().create();

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
      Server server, Member author, TextChannel channel, boolean forceRefreshCheck) {
    switch(objective) {
      case MASTERY_POINT_SPECIFIC_CHAMP:
      case AVERAGE_KDA_SPECIFIC_CHAMP:
      /*case WINRATE_SPECIFIC_CHAMP:*/
        return new SpecificChampionObjectiveDataHandler(objective, waiter, server, forceRefreshCheck, author, channel);
      /*case WINRATE_SPECIFIC_QUEUE:*/
      case SPECIFIC_QUEUE_RANK:
        return new SpecificQueueDataHandler(objective, waiter, server, forceRefreshCheck, author, channel);
      default:
        return new NoSpecificDataNeededHandler(objective, waiter, server, forceRefreshCheck, author, channel);
    }
  }

  public static String getShowableDeletionFormat(Leaderboard leaderboard, String language, TextChannel channel) {
    Objective objective = getObjectiveWithId(leaderboard.lead_type);

    String typeText = "type error";
    if(objective != null) {
      typeText = getLeaderboardTypeText(leaderboard, language, objective);
    }
    
    return String.format(LanguageManager.getText(language, "leaderboardShowableDeletionFormat"), typeText, channel.getAsMention());
  }

  private static String getLeaderboardTypeText(Leaderboard leaderboard, String language, Objective objective) {
    SpecificChamp specificChamp;

    if(leaderboard.lead_data == null || leaderboard.lead_data.equals("")) {
      return LanguageManager.getText(language, objective.getTranslationId());
    }

    String typeText = "type error";

    if(objective != null) {
      switch(objective) {
        case AVERAGE_KDA_SPECIFIC_CHAMP:
          specificChamp = gson.fromJson(leaderboard.lead_data, SpecificChamp.class);
          typeText = String.format(LanguageManager.getText(language, "leaderboardObjectiveAverageKDASpecifiedChamp"),
              specificChamp.getChampion().getEmoteUsable() + " " + specificChamp.getChampion().getName());
          break;
        case MASTERY_POINT_SPECIFIC_CHAMP:
          specificChamp = gson.fromJson(leaderboard.lead_data, SpecificChamp.class);
          typeText = String.format(LanguageManager.getText(language, "leaderboardObjectiveMasterPointSpecifiedChamp"),
              specificChamp.getChampion().getEmoteUsable() + " " + specificChamp.getChampion().getName());
          break;
        case SPECIFIC_QUEUE_RANK:
          QueueSelected queue = gson.fromJson(leaderboard.lead_data, QueueSelected.class);
          typeText = String.format(LanguageManager.getText(language, "leaderboardSpecifiedQueueRank"),
              LanguageManager.getText(language, GameQueueConfigId.getGameQueueWithQueueType(queue.getGameQueue().getApiName()).getNameId()));
          break;
        default:
          typeText = LanguageManager.getText(language, objective.getTranslationId());
          break;
      }
    }
    return typeText;
  }
}
