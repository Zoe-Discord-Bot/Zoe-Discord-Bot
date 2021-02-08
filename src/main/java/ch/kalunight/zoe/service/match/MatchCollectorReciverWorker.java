package ch.kalunight.zoe.service.match;

import java.util.concurrent.atomic.AtomicBoolean;

import ch.kalunight.zoe.model.MatchReceiver;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.constant.Platform;

public class MatchCollectorReciverWorker extends MatchReceiverWorker {

  private MatchReceiver matchReceiver;
  
  public MatchCollectorReciverWorker(MatchReceiver matchReceiver, AtomicBoolean gameLoadingConflict,
      MatchReference matchReference, Platform server, SummonerCache summoner) {
    super(gameLoadingConflict, matchReference, server, summoner);
    this.matchReceiver = matchReceiver;
  }
  
  @Override
  protected void runMatchReceveirWorker(SavedMatch matchCache) {
    matchReceiver.matchs.add(matchCache);
  }

}
