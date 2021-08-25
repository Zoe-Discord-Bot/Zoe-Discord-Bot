package ch.kalunight.zoe.service.match;

import ch.kalunight.zoe.model.MatchReceiver;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.riotapi.MatchKeyString;

public class MatchCollectorReciverWorker extends MatchReceiverWorker {

  private MatchReceiver matchReceiver;
  
  public MatchCollectorReciverWorker(MatchReceiver matchReceiver,
      MatchKeyString matchReference, ZoePlatform server, SavedSummoner summoner) {
    super(matchReference, server, summoner);
    this.matchReceiver = matchReceiver;
  }
  
  @Override
  protected void runMatchReceveirWorker(SavedMatch match) {
    if(matchReceiver.isGivenMatchWanted(match)) {
      matchReceiver.matchs.add(match);
    }
  }

}
