package ch.kalunight.zoe.service.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.riotapi.MatchKeyString;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public abstract class MatchReceiverWorker implements Runnable {

  private static final int CANCEL_GAME_DURATION = 500;
  
  protected static final List<MatchKeyString> matchsInWork = Collections.synchronizedList(new ArrayList<>());
  
  protected static final Logger logger = LoggerFactory.getLogger(MatchReceiverWorker.class);
  
  protected ZoePlatform server;
  
  protected MatchKeyString matchReference;
  
  protected Summoner summoner;
  
  protected MatchReceiverWorker(MatchKeyString matchReference, ZoePlatform server, Summoner summoner) {
    this.server = server;
    this.matchReference = matchReference;
    this.summoner = summoner;
    matchsInWork.add(matchReference);
  }
  
  @Override
  public void run() {
    logger.debug("Start to load game on server {}", server.getShowableName());
    LOLMatch match = null;
    try {
      match = Zoe.getRiotApi().getMatchById(matchReference.getPlatform(), matchReference.getMatchId());
    } catch (Exception e) {
      logger.warn("Unexpected error while getting match !", e);
    }
    
    try {
      if(match != null && match.getGameDuration() > CANCEL_GAME_DURATION) { // Check if the game has been canceled
        runMatchReceveirWorker(match);
      }
    }catch(Exception e){
      logger.error("Unexpected error in match receiver worker", e);
    }finally {
      matchsInWork.remove(matchReference);
    }
  }
  
  protected abstract void runMatchReceveirWorker(LOLMatch matchCache);
  
  public static void awaitAll(List<MatchKeyString> matchsToWait) {
    
    boolean needToWait;
    
    do {
      needToWait = false;
      for(MatchKeyString matchReference : matchsToWait) {
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
