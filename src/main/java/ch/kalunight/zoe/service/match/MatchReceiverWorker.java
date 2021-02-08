package ch.kalunight.zoe.service.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import ch.kalunight.zoe.riotapi.CachedRiotApi;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.constant.Platform;

public abstract class MatchReceiverWorker implements Runnable {

  private static final int CANCEL_GAME_DURATION = 500;
  
  protected static final List<MatchReference> matchsInWork = Collections.synchronizedList(new ArrayList<>());
  
  protected static final Logger logger = LoggerFactory.getLogger(MatchReceiverWorker.class);
  
  protected static final CachedRiotApi riotApi = Zoe.getRiotApi();
  
  protected AtomicBoolean gameLoadingConflict;
  
  protected Platform server;
  
  protected MatchReference matchReference;
  
  protected SummonerCache summoner;
  
  public MatchReceiverWorker(AtomicBoolean gameLoadingConflict,
      MatchReference matchReference, Platform server, DTO.SummonerCache summoner) {
    this.gameLoadingConflict = gameLoadingConflict;
    this.server = server;
    this.matchReference = matchReference;
    this.summoner = summoner;
    matchsInWork.add(matchReference);
  }
  
  @Override
  public void run() {
    logger.debug("Start to load game {} server {}", matchReference.getGameId(), server.getName());
    SavedMatch matchCache = null;
    try {
      matchCache = Zoe.getRiotApi().getMatchWithRateLimit(server, matchReference.getGameId());
    } catch (Exception e) {
      logger.warn("Unexpected error while getting match !", e);
    }
    
    if(matchCache != null) {
      //Add the call
      Zoe.getRiotApi().getAllMatchCounter().incrementAndGet();
    }
    
    try {
      if(matchCache != null && matchCache.getGameDurations() > CANCEL_GAME_DURATION) { // Check if the game has been canceled
        runMatchReceveirWorker(matchCache);
      }
    }catch(Exception e){
      logger.error("Unexpected error in match receiver worker", e);
    }finally {
      matchsInWork.remove(matchReference);
    }
  }
  
  protected abstract void runMatchReceveirWorker(SavedMatch matchCache);
  
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
