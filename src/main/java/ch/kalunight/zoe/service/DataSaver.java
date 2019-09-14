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

  private static final Logger logger = LoggerFactory.getLogger(DataSaver.class);

  private static DateTime nextSaveTime = DateTime.now().plusMinutes(TIME_BETWEEN_EACH_SAVE_IN_MINUTES);

  @Override
  public void run() {
    try {
      if(nextSaveTime.isBeforeNow()) {
        logger.info("Saving started !");
        Zoe.saveDataTxt();
        setNextSaveTime(DateTime.now().plusMinutes(TIME_BETWEEN_EACH_SAVE_IN_MINUTES));
        logger.info("Saving ended !");
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

}
