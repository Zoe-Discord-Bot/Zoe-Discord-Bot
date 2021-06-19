package ch.kalunight.zoe.service.match;

import ch.kalunight.zoe.model.MatchReceiverCondition;
import ch.kalunight.zoe.model.OldestGameChecker;
import ch.kalunight.zoe.model.WinRateReceiver;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import net.rithms.riot.constant.Platform;

public class MatchWinrateReceiverWorker extends MatchReceiverWorker {

  private WinRateReceiver winRateReceiver;

  public MatchWinrateReceiverWorker(WinRateReceiver winRateReceiver,
      String gameId, Platform server, SummonerCache summoner, MatchReceiverCondition matchCondition, OldestGameChecker gameChecker) {
    super(gameId, server, summoner, gameChecker, matchCondition);
    this.winRateReceiver = winRateReceiver;
  }

  @Override
  public void runMatchReceveirWorker(SavedMatch match) {
    try {
      if(match.isGivenAccountWinner(summoner.sumCache_summonerId)) {
        winRateReceiver.win.incrementAndGet();
      }else {
        winRateReceiver.loose.incrementAndGet();
      }

    }catch(Exception e) {
      logger.info("Unexpected Exception Error in MatchWinrateReceiver Worker: {}", e.getMessage());
    }
  }
}
