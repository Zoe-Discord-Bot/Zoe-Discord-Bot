package ch.kalunight.zoe.service;

import java.time.LocalDateTime;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerData;

public class DataSaver extends TimerTask {

  private static final int WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS = 10000;
  
  private static final int TIME_BETWEEN_EACH_DB_REFRESH = 12;
  
  private static final int TIME_BETWEEN_CLEAN_CACHE_IN_HOURS = 48;

  private static final Logger logger = LoggerFactory.getLogger(DataSaver.class);
  
  private static LocalDateTime nextCleanCacheTime = LocalDateTime.now().plusHours(1);
  
  private static LocalDateTime nextRefreshCacheDb = LocalDateTime.now().plusHours(3);

  @Override
  public void run() {
    try {
      if(nextCleanCacheTime.isBefore(LocalDateTime.now())) {
        setNextCleanCacheTime(LocalDateTime.now().plusHours(TIME_BETWEEN_CLEAN_CACHE_IN_HOURS));
        CleanCacheService cleanCacheThread = new CleanCacheService();
        ServerData.getServerExecutor().execute(cleanCacheThread);
      }
      
      if(nextRefreshCacheDb.isBefore(LocalDateTime.now())) {
        logger.info("Refresh cache started !");
        setNextRefreshCacheDb(LocalDateTime.now().plusHours(TIME_BETWEEN_EACH_DB_REFRESH));
        ServerData.getServerExecutor().submit(new SummonerCacheRefresh());
      }
      
    } catch(Exception e) {
      logger.error("Error : {}", e);
    } finally {
      TimerTask mainThread = new ServerChecker();
      ServerData.getServerCheckerThreadTimer().schedule(mainThread, WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS);
    }
  }
  
  private static void setNextCleanCacheTime(LocalDateTime nextCleanCacheTime) {
    DataSaver.nextCleanCacheTime = nextCleanCacheTime;
  }

  public static void setNextRefreshCacheDb(LocalDateTime nextRefreshCacheDb) {
    DataSaver.nextRefreshCacheDb = nextRefreshCacheDb;
  }


  
}
