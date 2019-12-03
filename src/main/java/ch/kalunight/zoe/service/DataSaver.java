package ch.kalunight.zoe.service;

import java.util.TimerTask;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.backup.BackupService;

public class DataSaver extends TimerTask {

  private static final int WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS = 10000;

  private static final int TIME_BETWEEN_EACH_SAVE_IN_MINUTES = 10;
  
  private static final int TIME_BETWEEN_EACH_BACKUP_IN_HOURS = 12;
  
  private static final int TIME_BETWEEN_CLEAN_CACHE_IN_HOURS = 48;

  private static final Logger logger = LoggerFactory.getLogger(DataSaver.class);

  private static DateTime nextSaveTime = DateTime.now().plusMinutes(TIME_BETWEEN_EACH_SAVE_IN_MINUTES);
  
  private static DateTime nextCleanCacheTime = DateTime.now().plusHours(1);
  
  private static DateTime nextBackupTime = DateTime.now().plusHours(1);

  @Override
  public void run() {
    try {
      if(nextCleanCacheTime.isBeforeNow()) {
        logger.info("Cleaning cache started !");
        setNextCleanCacheTime(DateTime.now().plusHours(TIME_BETWEEN_CLEAN_CACHE_IN_HOURS));
        Zoe.getRiotApi().cleanCache(); // Make it in another thread
        logger.info("Cleaning cache ended !");
      }
      
      if(nextBackupTime.isBeforeNow()) {
        logger.info("Backup started !");
        setNextBackupTime(DateTime.now().plusHours(TIME_BETWEEN_EACH_BACKUP_IN_HOURS));
        ServerData.getServerExecutor().execute(new BackupService());
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

  private static void setNextBackupTime(DateTime nextBackupTime) {
    DataSaver.nextBackupTime = nextBackupTime;
  }

  
}
