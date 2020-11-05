package ch.kalunight.zoe.service;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO.CurrentGameInfo;
import ch.kalunight.zoe.repositories.CurrentGameInfoRepository;

public class CleanCacheService implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(CleanCacheService.class);
  
  @Override
  public void run() {
    try {
      logger.info("Cleaning match cache started !");
      Stopwatch stopWatch = Stopwatch.createStarted();
      Zoe.getRiotApi().cleanCache();
      stopWatch.stop();
      logger.info("Cleaning match cache done in {} seconds !", stopWatch.elapsed(TimeUnit.SECONDS));
      
      
      logger.info("Cleaning match cache started !");
      stopWatch = Stopwatch.createStarted();
      List<CurrentGameInfo> currentGamesToDelete = CurrentGameInfoRepository.getCurrentGameUnreachable();
      
      for(CurrentGameInfo currentGameToDelete : currentGamesToDelete) {
        CurrentGameInfoRepository.deleteUnreachableCurrentGame(currentGameToDelete);;
      }
      
      stopWatch.stop();
      logger.info("Cleaning current match cache done in {} seconds !", stopWatch.elapsed(TimeUnit.SECONDS));
      
    } catch (SQLException e) {
      logger.error("SQL error in clean cache thread ! ", e);
    }
  }

}
