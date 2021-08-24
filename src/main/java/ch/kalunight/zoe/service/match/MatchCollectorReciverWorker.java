package ch.kalunight.zoe.service.match;

import ch.kalunight.zoe.model.MatchReceiver;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.riotapi.MatchKeyString;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class MatchCollectorReciverWorker extends MatchReceiverWorker {

  private MatchReceiver matchReceiver;
  
  public MatchCollectorReciverWorker(MatchReceiver matchReceiver,
      MatchKeyString matchReference, ZoePlatform server, Summoner summoner) {
    super(matchReference, server, summoner);
    this.matchReceiver = matchReceiver;
  }
  
  @Override
  protected void runMatchReceveirWorker(LOLMatch match) {
    if(matchReceiver.isGivenMatchWanted(match)) {
      matchReceiver.matchs.add(match);
    }
  }

}
