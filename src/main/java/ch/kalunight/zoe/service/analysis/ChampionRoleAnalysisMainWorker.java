 package ch.kalunight.zoe.service.analysis;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.repositories.ChampionRoleAnalysisRepository;
import ch.kalunight.zoe.repositories.SavedMatchCacheRepository;
import ch.kalunight.zoe.util.Ressources;
import net.rithms.riot.constant.Platform;

public class ChampionRoleAnalysisMainWorker implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ChampionRoleAnalysisMainWorker.class);
  
  private static final int MINIMUM_NUMBER_OF_MATCHS = 100;
  
  private static final int NORMAL_DRAFT_QUEUE_ID = 400;
  
  private static final double MINIMUM_POURCENTAGE_ROLE_COMMON = 5.0;
  
  private int championId;
  
  private AtomicInteger nbrTop = new AtomicInteger();
  private AtomicInteger nbrJng = new AtomicInteger();
  private AtomicInteger nbrMid = new AtomicInteger();
  private AtomicInteger nbrAdc = new AtomicInteger();
  private AtomicInteger nbrSup = new AtomicInteger();
  
  private AtomicInteger nbrMatch = new AtomicInteger();
  
  private AtomicInteger analysisDone = new AtomicInteger();
  
  public ChampionRoleAnalysisMainWorker(int championId) {
    this.championId = championId;
  }
  
  @Override
  public void run() {
    
    try {
      Champion champion = Ressources.getChampionDataById(championId);
      
      String version = SavedMatchCacheRepository.getCurrentLoLVersion(Platform.EUW); //Select EUW region for patch version, usually the last region to be patched.
      
      if(version == null) {
        return;
      }
      
      List<SavedMatch> matchsToAnalyse = SavedMatchCacheRepository.getMatchsByChampion(championId, GameQueueConfigId.SOLOQ.getId(), NORMAL_DRAFT_QUEUE_ID, version);
      
      int nbrMatchsToAnaylse = matchsToAnalyse.size();
      
      if(nbrMatchsToAnaylse < MINIMUM_NUMBER_OF_MATCHS) {
        return;
      }
      
      for(SavedMatch matchToAnalyse : matchsToAnalyse) {
        RoleMatchAnalysisWorker roleAnalysisWorker = new RoleMatchAnalysisWorker(matchToAnalyse, this);
        
        ServerThreadsManager.getDataAnalysisThread().execute(roleAnalysisWorker);
      }
      
      do {
        TimeUnit.MILLISECONDS.sleep(500);
      }while(nbrMatchsToAnaylse != analysisDone.get());
      
      double ratioTop = ((double) nbrTop.get() / nbrMatchsToAnaylse) * 100.0;
      double ratioJng = ((double) nbrJng.get() / nbrMatchsToAnaylse) * 100.0;
      double ratioMid = ((double) nbrMid.get() / nbrMatchsToAnaylse) * 100.0;
      double ratioAdc = ((double) nbrAdc.get() / nbrMatchsToAnaylse) * 100.0;
      double ratioSup = ((double) nbrSup.get() / nbrMatchsToAnaylse) * 100.0;
      
      String rolesList = getRolesString(ratioTop, ratioJng, ratioMid, ratioAdc, ratioSup);
      
      DTO.ChampionRoleAnalysis champRole = ChampionRoleAnalysisRepository.getChampionRoleAnalysis(championId);
      
      if(champRole == null) {
        ChampionRoleAnalysisRepository.createChampionRoles(championId, rolesList);
      }else {
        ChampionRoleAnalysisRepository.updateChampionsRoles(championId, rolesList);
      }

      if(champion != null) {
        List<ChampionRole> championsRoles = new ArrayList<>();
        
        String[] roles = rolesList.split(";");
        for(String strRole : roles) {
          ChampionRole role = ChampionRole.valueOf(strRole);
          
          if(role != null) {
            championsRoles.add(role);
          }
        }
        champion.setRoles(championsRoles);
      }
      
    } catch(SQLException e) {
      logger.error("SQL Error with a query", e);
    } catch (InterruptedException e) {
      logger.error("interupted exception !", e);
      Thread.currentThread().interrupt();
    }
    
  }

  private String getRolesString(double ratioTop, double ratioJng, double ratioMid, double ratioAdc, double ratioSup) {
    StringBuilder rolesListBuilder = new StringBuilder();
    
    if(ratioTop > MINIMUM_POURCENTAGE_ROLE_COMMON) {
      rolesListBuilder.append(ChampionRole.TOP.toString() + ";");
      logger.info("champion id {} detected as playable in top. Ratio of this role : {}%", championId, ratioTop);
    }
    
    if(ratioJng > MINIMUM_POURCENTAGE_ROLE_COMMON) {
      rolesListBuilder.append(ChampionRole.JUNGLE.toString() + ";");
      logger.info("champion id {} detected as playable in jng. Ratio of this role : {}%", championId, ratioJng);
    }
    
    if(ratioMid > MINIMUM_POURCENTAGE_ROLE_COMMON) {
      rolesListBuilder.append(ChampionRole.MID.toString() + ";");
      logger.info("champion id {} detected as playable in mid. Ratio of this role : {}%", championId, ratioMid);
    }
    
    if(ratioAdc > MINIMUM_POURCENTAGE_ROLE_COMMON) {
      rolesListBuilder.append(ChampionRole.ADC.toString() + ";");
      logger.info("champion id {} detected as playable in adc. Ratio of this role : {}%", championId, ratioAdc);
    }
    
    if(ratioSup > MINIMUM_POURCENTAGE_ROLE_COMMON) {
      rolesListBuilder.append(ChampionRole.SUPPORT.toString() + ";");
      logger.info("champion id {} detected as playable in sup. Ratio of this role : {}%", championId, ratioSup);
    }
    
    return rolesListBuilder.toString();
  }

  public int getChampionId() {
    return championId;
  }

  public AtomicInteger getNbrTop() {
    return nbrTop;
  }

  public AtomicInteger getNbrJng() {
    return nbrJng;
  }

  public AtomicInteger getNbrMid() {
    return nbrMid;
  }

  public AtomicInteger getNbrAdc() {
    return nbrAdc;
  }

  public AtomicInteger getNbrSup() {
    return nbrSup;
  }

  public AtomicInteger getNbrMatch() {
    return nbrMatch;
  }

  public AtomicInteger getAnalysisDone() {
    return analysisDone;
  }
  
}
