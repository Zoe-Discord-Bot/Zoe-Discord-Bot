package ch.kalunight.zoe.model.team;

import java.util.List;

import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.service.analysis.TeamBanAnalysisWorker;
import ch.kalunight.zoe.util.TeamUtil;
import net.dv8tion.jda.api.entities.TextChannel;

public class TeamSelectorAnalysisDataManager extends TeamSelectorDataManager {

  public TeamSelectorAnalysisDataManager(Server server, TextChannel channel, boolean forceRefresh) {
    super(server, channel, forceRefresh);
  }

  @Override
  public void treatData() {
    List<TeamPlayerAnalysisDataCollector> playersData = TeamUtil.getTeamPlayersDataWithAnalysisDoneWithAccountData(accountsToTreat, forceRefresh);
    
    TeamBanAnalysisWorker banAnalysisWorker = new TeamBanAnalysisWorker(server, null, null, channel, playersData);
    banAnalysisWorker.run();
  }

}
