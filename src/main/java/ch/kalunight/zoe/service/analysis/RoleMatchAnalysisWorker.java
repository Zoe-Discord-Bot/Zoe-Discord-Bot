package ch.kalunight.zoe.service.analysis;

import java.util.concurrent.atomic.AtomicInteger;

import ch.kalunight.zoe.model.dto.SavedMatch;

public class RoleMatchAnalysisWorker implements Runnable {

  private SavedMatch match;
  
  private ChampionRoleAnalysis mainAnalyser;
  
  public RoleMatchAnalysisWorker(SavedMatch match, ChampionRoleAnalysis mainAnalyser) {
    this.match = match;
    this.mainAnalyser = mainAnalyser;
  }
  
  @Override
  public void run() {
    
    
  }

}
