package ch.kalunight.zoe.service;

import java.util.TimerTask;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;

public class DataSaver extends TimerTask {

  private static final int WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS = 10000;

  private static final int TIME_BETWEEN_EACH_SAVE_IN_MINUTES = 10;
  
  private static final int TIME_BETWEEN_CLEAN_CACHE_IN_HOURS = 48;

  private static final Logger logger = LoggerFactory.getLogger(DataSaver.class);

  private static DateTime nextSaveTime = DateTime.now().plusMinutes(TIME_BETWEEN_EACH_SAVE_IN_MINUTES);
  
  private static DateTime nextCleanCacheTime = DateTime.now();

  @Override
  public void run() {
    try {
      if(nextSaveTime.isBeforeNow()) {
        logger.info("Saving started !");
        setNextSaveTime(DateTime.now().plusMinutes(TIME_BETWEEN_EACH_SAVE_IN_MINUTES));
        Zoe.saveDataTxt();
        logger.info("Saving ended !");
      }
      
      if(nextCleanCacheTime.isBeforeNow()) {
        logger.info("Cleaning cache started !");
        setNextCleanCacheTime(DateTime.now().plusHours(TIME_BETWEEN_CLEAN_CACHE_IN_HOURS));
        Zoe.getRiotApi().cleanCache();
        logger.info("Cleaning cache ended !");
      }
    } catch(Exception e) {
      logger.error("Error : {}", e);
    } finally {
      TimerTask mainThread = new ServerChecker();
      ServerData.getServerCheckerThreadTimer().schedule(mainThread, WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS);
    }
  }

  private static void setNextSaveTime(DateTime nextSaveTime) {
    DataSaver.nextSaveTime = nextSaveTime;
  }

  private static void setNextCleanCacheTime(DateTime nextCleanCacheTime) {
    DataSaver.nextCleanCacheTime = nextCleanCacheTime;
  }

}
