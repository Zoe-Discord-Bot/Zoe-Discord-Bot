package ch.kalunight.zoe.service.match;

import java.util.concurrent.atomic.AtomicBoolean;

import ch.kalunight.zoe.model.WinRateReceiver;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.constant.Platform;

public class MatchWinrateReceiverWorker extends MatchReceiverWorker {

  private WinRateReceiver winRateReceiver;

  public MatchWinrateReceiverWorker(WinRateReceiver winRateReceiver, AtomicBoolean gameLoadingConflict,
      MatchReference matchReference, Platform server, SummonerCache summoner) {
    super(gameLoadingConflict, matchReference, server, summoner);
    this.winRateReceiver = winRateReceiver;
  }

  @Override
  public void runMatchReceveirWorker(SavedMatch match) {
    try {
      if(!match.isGivenAccountWinner(summoner.sumCache_summonerId)) {
        winRateReceiver.win.incrementAndGet();
      }else {
        winRateReceiver.loose.incrementAndGet();
      }

    }catch(Exception e) {
      logger.info("Unexpected Exception Error : {}", e.getMessage());
    }
  }
}
