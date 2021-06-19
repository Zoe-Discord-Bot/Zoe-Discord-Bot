package ch.kalunight.zoe.service.match;

import ch.kalunight.zoe.model.MatchReceiver;
import ch.kalunight.zoe.model.MatchReceiverCondition;
import ch.kalunight.zoe.model.OldestGameChecker;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import net.rithms.riot.constant.Platform;

public class MatchCollectorReciverWorker extends MatchReceiverWorker {

  private MatchReceiver matchReceiver;
  
  public MatchCollectorReciverWorker(MatchReceiver matchReceiver,
      String gameId, Platform server, SummonerCache summoner, MatchReceiverCondition matchCondition, OldestGameChecker oldestGameChecker) {
    super(gameId, server, summoner, oldestGameChecker, matchCondition);
    this.matchReceiver = matchReceiver;
  }
  
  @Override
  protected void runMatchReceveirWorker(SavedMatch matchCache) {
    matchReceiver.matchs.add(matchCache);
  }

}
