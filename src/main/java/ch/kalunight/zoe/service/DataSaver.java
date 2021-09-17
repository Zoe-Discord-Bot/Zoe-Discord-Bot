package ch.kalunight.zoe.service;

import java.time.LocalDateTime;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.service.analysis.ChampionRoleAnalysisMainWorker;
import ch.kalunight.zoe.service.infochannel.InfoPanelRefresher;
import ch.kalunight.zoe.util.Ressources;

public class DataSaver extends TimerTask {

  private static final int WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS = 10000;

  private static final int TIME_BETWEEN_EACH_CHAMPION_ROLE_REFRESH_IN_HOURS = 72;
  
  private static final int TIME_BETWEEN_SERVERS_PLAYERS_CLEANING = 24;

  private static final int TIME_BETWEEN_EACH_SUB_REFRESH_IN_MINUTES = 30;
  
  private static final Logger logger = LoggerFactory.getLogger(DataSaver.class);
  
  private static final boolean CHAMPION_ANALYSIS_ALLOWED = true;
  
  private static LocalDateTime nextRefreshChampionsRole = LocalDateTime.now().plusHours(12);
  
  private static LocalDateTime nextRefreshServerPlayersCleaning = LocalDateTime.now().plusHours(TIME_BETWEEN_SERVERS_PLAYERS_CLEANING);

  private static LocalDateTime nextRefreshSubscription = LocalDateTime.now();

  
  @Override
  public void run() {
    try {
      if(nextRefreshChampionsRole.isBefore(LocalDateTime.now()) && CHAMPION_ANALYSIS_ALLOWED) {
        logger.info("Refresh champion roles started !");
        setNextRefreshChampionRole(LocalDateTime.now().plusHours(TIME_BETWEEN_EACH_CHAMPION_ROLE_REFRESH_IN_HOURS));
        
        for(Champion champion : Ressources.getChampions()) {
          ServerThreadsManager.getDataAnalysisManager().submit(new ChampionRoleAnalysisMainWorker(champion.getKey())); 
        }
      }
      
      if(nextRefreshServerPlayersCleaning.isBefore(LocalDateTime.now())) {
        logger.info("Refresh champion roles started !");
        setNextRefreshServerPlayersCleaning(LocalDateTime.now().plusHours(TIME_BETWEEN_SERVERS_PLAYERS_CLEANING));
        
        InfoPanelRefresher.getServerswhereplayersalreadychecked().clear();
      }
      
      if(nextRefreshSubscription.isBefore(LocalDateTime.now())) {
        logger.info("Refresh subscriptions started !");
        nextRefreshSubscription = LocalDateTime.now().plusMinutes(TIME_BETWEEN_EACH_SUB_REFRESH_IN_MINUTES);
        
        ZoeUserRoleService subRefreshService = new ZoeUserRoleService();
        ServerThreadsManager.getServerExecutor().execute(subRefreshService);
      }
      
    } catch(Exception e) {
      logger.error("Error in dataSaver : {}", e.getMessage(), e);
    } finally {
      TimerTask mainThread = new ServerChecker();
      ServerThreadsManager.getServerCheckerThreadTimer().schedule(mainThread, WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS);
      logger.info("Data saver thread done !");
    }
  }

  private static void setNextRefreshChampionRole(LocalDateTime nextRefreshChampionsRole) {
    DataSaver.nextRefreshChampionsRole = nextRefreshChampionsRole;
  }

  public static void setNextRefreshServerPlayersCleaning(LocalDateTime nextRefreshServerPlayersCleaning) {
    DataSaver.nextRefreshServerPlayersCleaning = nextRefreshServerPlayersCleaning;
  }
  
}
