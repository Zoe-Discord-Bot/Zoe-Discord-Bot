package ch.kalunight.zoe.model.clash;

import java.util.Comparator;

import ch.kalunight.zoe.model.dto.SavedClashTournament;
import ch.kalunight.zoe.model.dto.SavedClashTournamentPhase;

public class ClashTournamentComparator implements Comparator<SavedClashTournament> {

  @Override
  public int compare(SavedClashTournament o1, SavedClashTournament o2) {
    
    //We compare the first phase with the other first phase of each.
    
    SavedClashTournamentPhase firstPhaseO1 = o1.getSchedule().get(0);
    
    SavedClashTournamentPhase firstPhase02 = o2.getSchedule().get(0);
    
    if(firstPhaseO1.getRegistrationTime() == firstPhase02.getRegistrationTime()) {
      return 0;
    }
    
    if(firstPhaseO1.getRegistrationTime() < firstPhase02.getRegistrationTime()) {
      return -1;
    }
    
    if(firstPhaseO1.getRegistrationTime() > firstPhase02.getRegistrationTime()) {
      return 1;
    }
    
    return 0;
  }

}
