package ch.kalunight.zoe.service.match;

import ch.kalunight.zoe.model.KDAReceiver;
import ch.kalunight.zoe.model.MatchReceiverCondition;
import ch.kalunight.zoe.model.OldestGameChecker;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import net.rithms.riot.constant.Platform;

public class MatchKDAReceiverWorker extends MatchReceiverWorker {

  private KDAReceiver kdaReceiver;

  public MatchKDAReceiverWorker(KDAReceiver kdaReceiver, String gameId, Platform server,
      SummonerCache summoner, MatchReceiverCondition matchCondition, OldestGameChecker gameChecker) {
    super(gameId, server, summoner, gameChecker, matchCondition);
    this.kdaReceiver = kdaReceiver;
  }

  @Override
  protected void runMatchReceveirWorker(SavedMatch matchCache) {
    try {
      SavedMatchPlayer player = matchCache.getSavedMatchPlayerBySummonerId(summoner.sumCache_summonerId);
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
