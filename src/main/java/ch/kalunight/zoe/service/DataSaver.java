package ch.kalunight.zoe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.Zoe;

public class DataSaver implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(DataSaver.class);

  @Override
  public void run() {
    logger.info("Start saving ...");
    try {
      Zoe.saveDataTxt();
      logger.info("Saving ended !");
    } catch(Exception e) {
      logger.error("Error : {}", e);
    }
  }

}
