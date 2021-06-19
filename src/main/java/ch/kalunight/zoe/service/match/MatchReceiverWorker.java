package ch.kalunight.zoe.service.match;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.MatchReceiverCondition;
import ch.kalunight.zoe.model.OldestGameChecker;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import ch.kalunight.zoe.riotapi.CachedRiotApi;
import net.rithms.riot.constant.Platform;

public abstract class MatchReceiverWorker implements Runnable {

  private static final int CANCEL_GAME_DURATION = 500;

  protected static final List<String> matchsInWork = Collections.synchronizedList(new ArrayList<>());

  protected static final Logger logger = LoggerFactory.getLogger(MatchReceiverWorker.class);

  protected static final CachedRiotApi riotApi = Zoe.getRiotApi();

  protected Platform server;

  protected String matchId;

  protected SummonerCache summoner;

  private MatchReceiverCondition matchCondition;

  private boolean matchWanted;
  
  private OldestGameChecker oldestGameChecker;

  public MatchReceiverWorker(String matchId, Platform server, DTO.SummonerCache summoner, OldestGameChecker oldestGameChecker, MatchReceiverCondition matchCondition) {
    this.server = server;
    this.matchId = matchId;
    this.summoner = summoner;
    this.matchCondition = matchCondition;
    this.matchWanted = false;
    this.oldestGameChecker = oldestGameChecker;
    matchsInWork.add(matchId);
  }

  @Override
  public void run() {
    logger.debug("Start to load game {} server {}", matchId, server.getName());
    SavedMatch matchCache = null;
    try {
      matchCache = Zoe.getRiotApi().getMatchWithRateLimit(server, matchId);
    } catch (Exception e) {
      logger.warn("Unexpected error while getting match !", e);
    }

    if(matchCache != null) {
      //Add the call
      Zoe.getRiotApi().getAllMatchCounter().incrementAndGet();
      
      oldestGameChecker.updateOldestGameDateTime(new Timestamp(matchCache.getGameCreation()).toLocalDateTime());
    }

    if(matchCache != null && matchCondition.isGivenMatchWanted(matchCache)) {
      matchWanted = true;
    }

    try {
      if(matchCache != null && matchCache.getGameDurations() > CANCEL_GAME_DURATION && matchWanted) { // Check if the game has been canceled
        runMatchReceveirWorker(matchCache);
      }
    }catch(Exception e){
      logger.error("Unexpected error in match receiver worker", e);
    }finally {
      matchsInWork.remove(matchId);
    }
  }

  protected abstract void runMatchReceveirWorker(SavedMatch matchCache);

  public static void awaitAll(List<String> matchsToWait) {

    boolean needToWait;

    do {
      needToWait = false;
      for(String gameId : matchsToWait) {
        if(matchsInWork.contains(gameId)) {
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
