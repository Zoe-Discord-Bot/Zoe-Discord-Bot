package ch.kalunight.zoe.service;

import java.time.LocalDateTime;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.service.analysis.ChampionRoleAnalysisMainWorker;
import ch.kalunight.zoe.util.Ressources;

public class DataSaver extends TimerTask {

  private static final int WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS = 10000;

  private static final int TIME_BETWEEN_EACH_CHAMPION_ROLE_REFRESH_IN_HOURS = 12;
  
  private static final int TIME_BETWEEN_CLEAN_CACHE_IN_HOURS = 48;
  
  private static final int TIME_BETWEEN_EACH_CLASH_TOURNAMENT_DB_IN_HOUURS = 4;

  private static final Logger logger = LoggerFactory.getLogger(DataSaver.class);
  
  private static LocalDateTime nextCleanCacheTime = LocalDateTime.now().plusHours(1);
  
  private static LocalDateTime nextRefreshChampionsRole = LocalDateTime.now().plusHours(1);
  
  private static LocalDateTime nextRefreshClashTournament = LocalDateTime.now().plusHours(TIME_BETWEEN_EACH_CLASH_TOURNAMENT_DB_IN_HOUURS);

  @Override
  public void run() {
    try {
      if(nextCleanCacheTime.isBefore(LocalDateTime.now())) {
        setNextCleanCacheTime(LocalDateTime.now().plusHours(TIME_BETWEEN_CLEAN_CACHE_IN_HOURS));
        CleanCacheService cleanCacheThread = new CleanCacheService();
        ServerThreadsManager.getServerExecutor().execute(cleanCacheThread);
      }

      if(nextRefreshChampionsRole.isBefore(LocalDateTime.now())) {
        logger.info("Refresh champion roles started !");
        setNextRefreshChampionRole(LocalDateTime.now().plusHours(TIME_BETWEEN_EACH_CHAMPION_ROLE_REFRESH_IN_HOURS));
        
        for(Champion champion : Ressources.getChampions()) {
          ServerThreadsManager.getDataAnalysisManager().submit(new ChampionRoleAnalysisMainWorker(champion.getKey())); 
        }
      }
      
      if(nextRefreshClashTournament.isBefore(LocalDateTime.now())) {
        logger.info("Refresh clash tournament cash roles started !");
        setNextRefreshChampionRole(LocalDateTime.now().plusHours(TIME_BETWEEN_EACH_CHAMPION_ROLE_REFRESH_IN_HOURS));
        
        
      }
      
      
    } catch(Exception e) {
      logger.error("Error in dataSaver : {}", e.getMessage(), e);
    } finally {
      TimerTask mainThread = new ServerChecker();
      ServerThreadsManager.getServerCheckerThreadTimer().schedule(mainThread, WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS);
    }
  }
  
  private static void setNextCleanCacheTime(LocalDateTime nextCleanCacheTime) {
    DataSaver.nextCleanCacheTime = nextCleanCacheTime;
  }

  private static void setNextRefreshChampionRole(LocalDateTime nextRefreshChampionsRole) {
    DataSaver.nextRefreshChampionsRole = nextRefreshChampionsRole;
  }
  
}
