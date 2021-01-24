package ch.kalunight.zoe.model.team;

import java.util.List;

import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.service.analysis.TeamBanAnalysisWorker;
import ch.kalunight.zoe.util.TeamUtil;

public class TeamSelectorAnalysisDataManager extends TeamSelectorDataManager {

  public TeamSelectorAnalysisDataManager(CommandEvent event, Server server) {
    super(event, server);
  }

  @Override
  public void treatData() {
    List<TeamPlayerAnalysisDataCollector> playersData = TeamUtil.getTeamPlayersDataWithAnalysisDoneWithAccountData(accountsToTreat);
    
    TeamBanAnalysisWorker banAnalysisWorker = new TeamBanAnalysisWorker(server, null, null, baseEvent.getTextChannel(), playersData);
    banAnalysisWorker.run();
  }

}
