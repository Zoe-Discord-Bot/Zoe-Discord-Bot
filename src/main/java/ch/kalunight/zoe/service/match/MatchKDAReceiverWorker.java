package ch.kalunight.zoe.service.match;

import java.util.concurrent.atomic.AtomicBoolean;

import ch.kalunight.zoe.model.KDAReceiver;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.constant.Platform;

public class MatchKDAReceiverWorker extends MatchReceiverWorker {

  private KDAReceiver kdaReceiver;

  public MatchKDAReceiverWorker(KDAReceiver kdaReceiver, AtomicBoolean gameLoadingConflict, MatchReference matchReference, Platform server,
      SavedSummoner summoner) {
    super(gameLoadingConflict, matchReference, server, summoner);
    this.kdaReceiver = kdaReceiver;
  }

  @Override
  protected void runMatchReceveirWorker(SavedMatch matchCache) {
    try {
      SavedMatchPlayer player = matchCache.getSavedMatchPlayerByAccountId(summoner.getAccountId());
      if(player != null) {
        kdaReceiver.numberOfMatchs.incrementAndGet();
        kdaReceiver.kills.addAndGet(player.getKills());
        kdaReceiver.deaths.addAndGet(player.getDeaths());
        kdaReceiver.assists.addAndGet(player.getAssists());
      }
    }catch(Exception e) {
      logger.info("Unexpected error : {}", e.getMessage());
    }
  }

}
