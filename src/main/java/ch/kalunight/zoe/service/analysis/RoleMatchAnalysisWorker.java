package ch.kalunight.zoe.service.analysis;

import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;
import no.stelar7.api.r4j.basic.constants.types.lol.LaneType;
import no.stelar7.api.r4j.basic.constants.types.lol.RoleType;

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
        
        ChampionRole role = ChampionRole.getChampionRoleWithLaneAndRole(LaneType.valueOf(player.getLane()), RoleType.valueOf(player.getRole()));
        
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
        
        mainAnalyser.getKills().addAndGet(player.getKills());
        mainAnalyser.getDeaths().addAndGet(player.getDeaths());
        mainAnalyser.getAssists().addAndGet(player.getAssists());
      }
      
    }finally {
      mainAnalyser.getAnalysisDone().incrementAndGet();
    }
  }

}
