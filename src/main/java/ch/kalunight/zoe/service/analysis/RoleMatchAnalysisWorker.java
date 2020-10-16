package ch.kalunight.zoe.service.analysis;

import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;

public class RoleMatchAnalysisWorker implements Runnable {

  private SavedMatch match;
  
  private ChampionRoleAnalysisMainWorker mainAnalyser;
  
  public RoleMatchAnalysisWorker(SavedMatch match, ChampionRoleAnalysisMainWorker mainAnalyser) {
    this.match = match;
    this.mainAnalyser = mainAnalyser;
  }
  
  @Override
  public void run() {
    try {
      
      SavedMatchPlayer player = match.getSavedMatchPlayerByChampionId(mainAnalyser.getChampionId());
      if(player != null) {
        
        ChampionRole role = ChampionRole.getChampionRoleWithLaneAndRole(player.getLane(), player.getRole());
        
        if(role != null) {
          switch(role) {
          case ADC:
            mainAnalyser.getNbrAdc().incrementAndGet();
            break;
          case JUNGLE:
            mainAnalyser.getNbrJng().incrementAndGet();
            break;
          case MID:
            mainAnalyser.getNbrMid().incrementAndGet();
            break;
          case SUPPORT:
            mainAnalyser.getNbrSup().incrementAndGet();
            break;
          case TOP:
            mainAnalyser.getNbrTop().incrementAndGet();
            break;
          }
          
          mainAnalyser.getNbrMatch().incrementAndGet();
        }
      }
      
    }finally {
      mainAnalyser.getAnalysisDone().incrementAndGet();
    }
  }

}
