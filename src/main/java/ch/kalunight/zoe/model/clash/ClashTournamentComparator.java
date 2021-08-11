package ch.kalunight.zoe.model.clash;

import java.util.Comparator;

import no.stelar7.api.r4j.pojo.lol.clash.ClashTournament;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTournamentPhase;

public class ClashTournamentComparator implements Comparator<ClashTournament> {

  @Override
  public int compare(ClashTournament o1, ClashTournament o2) {
    
    //We compare the first phase with the other first phase of each.
    
    ClashTournamentPhase firstPhaseO1 = o1.getSchedule().get(0);
    
    ClashTournamentPhase firstPhase02 = o2.getSchedule().get(0);
    
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
