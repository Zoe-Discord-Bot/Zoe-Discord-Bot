package ch.kalunight.zoe.model.team;

import java.util.Collections;
import java.util.List;

import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.TeamUtil;

public class TeamSelectorPredictRoleDataManager extends TeamSelectorDataManager {

  public TeamSelectorPredictRoleDataManager(CommandEvent event, Server server) {
    super(event, server);
  }

  @Override
  public void treatData() {
    List<TeamPlayerAnalysisDataCollector> playersData = TeamUtil.loadAllPlayersDataWithAccountData(accountsToTreat);
    
    TeamUtil.determineRole(playersData);
    
    Collections.sort(playersData);

    StringBuilder builder = new StringBuilder();
    builder.append(LanguageManager.getText(server.getLanguage(), "statsPredictRoleTitleDeterminedRole") + "\n");
    for(TeamPlayerAnalysisDataCollector playerToShow : playersData) {
      builder.append(LanguageManager.getText(server.getLanguage(), TeamUtil.getChampionRoleAbrID(playerToShow.getFinalDeterminedPosition())) + " : *" 
          + playerToShow.getPlatform().getName().toUpperCase() + "* " + playerToShow.getSummoner().getSumCacheData().getName() + "\n");
    }

    builder.append("*" + LanguageManager.getText(server.getLanguage(), "disclaimerAnalysis") + "*");
    
    baseEvent.reply(builder.toString());
  }

}
