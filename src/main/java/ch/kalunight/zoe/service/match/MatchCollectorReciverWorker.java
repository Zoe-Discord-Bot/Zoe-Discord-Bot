package ch.kalunight.zoe.service.match;

import ch.kalunight.zoe.model.MatchReceiver;
import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;
import no.stelar7.api.r4j.impl.lol.builders.matchv5.match.MatchBuilder;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class MatchCollectorReciverWorker extends MatchReceiverWorker {

  private MatchReceiver matchReceiver;
  
  public MatchCollectorReciverWorker(MatchReceiver matchReceiver,
      MatchBuilder matchReference, LeagueShard server, Summoner summoner) {
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
