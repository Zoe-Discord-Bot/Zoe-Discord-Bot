package ch.kalunight.zoe.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.WinRateReceiver;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.riotapi.CacheManager;
import ch.kalunight.zoe.riotapi.CachedRiotApi;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class MatchReceiverWorker implements Runnable {

  private static final List<MatchReference> matchsInWork = Collections.synchronizedList(new ArrayList<>());
  
  private static final Logger logger = LoggerFactory.getLogger(MatchReceiverWorker.class);
  
  private static final CachedRiotApi riotApi = Zoe.getRiotApi();
  
  private WinRateReceiver winRateReceiver;
  
  private AtomicBoolean gameLoadingConflict;
  
  private Platform server;
  
  private MatchReference matchReference;
  
  private Summoner summoner;
  
  public MatchReceiverWorker(WinRateReceiver winRateReceiver, AtomicBoolean gameLoadingConflict,
      MatchReference matchReference, Platform server, Summoner summoner) {
    this.winRateReceiver = winRateReceiver;
    this.gameLoadingConflict = gameLoadingConflict;
    this.server = server;
    this.matchReference = matchReference;
    this.summoner = summoner;
    matchsInWork.add(matchReference);
  }

  @Override
  public void run() {
    try {
      logger.debug("Start to load game {} server {}", matchReference.getGameId(), server.getName());
      DTO.MatchCache matchCache = Zoe.getRiotApi().getCachedMatch(server, matchReference.getGameId());

      if(matchCache != null) {
        SavedMatch cacheMatch = matchCache.mCatch_savedMatch;

        if(cacheMatch.isGivenAccountWinner(summoner.getAccountId())) {
          winRateReceiver.win.incrementAndGet();
        }else {
          winRateReceiver.loose.incrementAndGet();
        }
      }else {
          Match match = riotApi.getMatchWithRateLimit(server, matchReference.getGameId());

          if(match == null) {
            return;
          }
          
          Participant participant = match.getParticipantByAccountId(summoner.getAccountId());

          if(participant != null && participant.getTimeline().getCreepsPerMinDeltas() != null) { // Check if the game has been canceled

            String result = match.getTeamByTeamId(participant.getTeamId()).getWin();
            if(result.equalsIgnoreCase("Fail")) {
              winRateReceiver.loose.incrementAndGet();
              CacheManager.createCacheMatch(server, match);
            }

            if(result.equalsIgnoreCase("Win")) {
              winRateReceiver.win.incrementAndGet();
              CacheManager.createCacheMatch(server, match);
            }
          }
      }
      
    }catch(SQLException e) {
      logger.info("SQL error (unique constraint error, normaly nothing severe) Error : {}", e.getMessage());
      gameLoadingConflict.set(true);
    }catch(Exception e){
      logger.error("Unexpected error in match receiver worker", e);
    }finally {
      matchsInWork.remove(matchReference);
    }
  }
  
  public static void awaitAll(List<MatchReference> matchsToWait) {
    
    boolean needToWait;
    
    do {
      needToWait = false;
      for(MatchReference matchReference : matchsToWait) {
        if(matchsInWork.contains(matchReference)) {
          needToWait = true;
          break;
        }
      }
      
      if(needToWait) {
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
          logger.error("Thread as been interupt when waiting Match Worker !", e);
          Thread.currentThread().interrupt();
        }
      }
      
    }while(needToWait);
  }

}
