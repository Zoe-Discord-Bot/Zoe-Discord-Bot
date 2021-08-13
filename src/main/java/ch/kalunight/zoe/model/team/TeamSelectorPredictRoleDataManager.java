package ch.kalunight.zoe.model.team;

import java.util.Collections;
import java.util.List;

import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.TeamUtil;
import net.dv8tion.jda.api.entities.TextChannel;

public class TeamSelectorPredictRoleDataManager extends TeamSelectorDataManager {

  public TeamSelectorPredictRoleDataManager(Server server, TextChannel channel) {
    super(server, channel);
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
          + playerToShow.getPlatform().getRealmValue().toUpperCase() + "* " + playerToShow.getSummoner().getName() + "\n");
    }

    builder.append("*" + LanguageManager.getText(server.getLanguage(), "disclaimerAnalysis") + "*");
    
    channel.sendMessage(builder.toString()).queue();
  }

}
