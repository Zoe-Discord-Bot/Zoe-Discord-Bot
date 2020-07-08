package ch.kalunight.zoe.service.match;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.kalunight.zoe.model.KDAReceiver;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;
import ch.kalunight.zoe.model.dto.DTO.MatchCache;
import ch.kalunight.zoe.riotapi.CacheManager;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.match.dto.ParticipantStats;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class MatchKDAReceiverWorker extends MatchReceiverWorker {

  private KDAReceiver kdaReceiver;

  public MatchKDAReceiverWorker(KDAReceiver kdaReceiver, AtomicBoolean gameLoadingConflict, MatchReference matchReference, Platform server,
      Summoner summoner) {
    super(gameLoadingConflict, matchReference, server, summoner);
    this.kdaReceiver = kdaReceiver;
  }

  @Override
  protected void runMatchReceveirWorker(MatchCache matchCache) {
    try {
      if(matchCache != null) {
        SavedMatch cacheMatch = matchCache.mCatch_savedMatch;
        SavedMatchPlayer player = cacheMatch.getSavedMatchPlayerByAccountId(summoner.getAccountId());
        if(player != null) {
          kdaReceiver.numberOfMatchs.incrementAndGet();
          kdaReceiver.kills.addAndGet(player.getKills());
          kdaReceiver.deaths.addAndGet(player.getDeaths());
          kdaReceiver.assists.addAndGet(player.getAssists());
        }
      }else {
        Match match = riotApi.getMatchWithRateLimit(server, matchReference.getGameId());

        if(match == null) {
          return;
        }

        Participant participant = match.getParticipantByAccountId(summoner.getAccountId());
        if(participant != null) {
          ParticipantStats participantStats = participant.getStats();
          if(participantStats != null && participant.getTimeline().getCreepsPerMinDeltas() != null) { // Check if the game has been canceled
            kdaReceiver.numberOfMatchs.incrementAndGet();
            kdaReceiver.kills.addAndGet(participantStats.getKills());
            kdaReceiver.deaths.addAndGet(participantStats.getDeaths());
            kdaReceiver.assists.addAndGet(participantStats.getAssists());
            
            CacheManager.createCacheMatch(server, match);
          }
        }
      }

    }catch(SQLException e) {
      logger.info("SQL error (unique constraint error, normaly nothing severe) Error : {}", e.getMessage());
      gameLoadingConflict.set(true);
    }
  }

}
