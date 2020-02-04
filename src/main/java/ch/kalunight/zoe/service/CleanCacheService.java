package ch.kalunight.zoe.service;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;

public class CleanCacheService implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(CleanCacheService.class);
  
  @Override
  public void run() {
    try {
      logger.info("Cleaning cache started !");
      Zoe.getRiotApi().cleanCache();
      logger.info("Cleaning cache ended !");
    } catch (SQLException e) {
      logger.error("SQL error in clean cache thread ! ", e);
    }
  }

}
