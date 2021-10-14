package ch.kalunight.zoe.service.analysis;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportKDA;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.repositories.ChampionRoleAnalysisRepository;
import ch.kalunight.zoe.util.Ressources;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;

public class ChampionRoleAnalysisMainWorker implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ChampionRoleAnalysisMainWorker.class);

  private static final DecimalFormat STATS_FORMAT = new DecimalFormat("###.##");

  private static final int MINIMUM_NUMBER_OF_MATCHS = 100;

  private static final double MINIMUM_POURCENTAGE_ROLE_COMMON = 5.0;

  private int championId;

  private Champion champion;

  private AtomicInteger nbrTop = new AtomicInteger();
  private AtomicInteger nbrJng = new AtomicInteger();
  private AtomicInteger nbrMid = new AtomicInteger();
  private AtomicInteger nbrAdc = new AtomicInteger();
  private AtomicInteger nbrSup = new AtomicInteger();

  private AtomicInteger kills = new AtomicInteger();
  private AtomicInteger deaths = new AtomicInteger();
  private AtomicInteger assists = new AtomicInteger();

  private AtomicInteger nbrMatch = new AtomicInteger();

  private AtomicInteger analysisDone = new AtomicInteger();

  public ChampionRoleAnalysisMainWorker(int championId) {
    this.championId = championId;
  }

  @Override
  public void run() {

    try {
      champion = Ressources.getChampionDataById(championId);

      String version = Zoe.getRiotApi().getLastPatchVersion();

      if(version == null) {
        return;
      }
      
      List<SavedMatch> matchsToAnalyse = Zoe.getRiotApi().getMatchsByChampionId(championId, GameQueueType.TEAM_BUILDER_RANKED_SOLO);

      int nbrMatchsToAnaylse = matchsToAnalyse.size();

      if(nbrMatchsToAnaylse < MINIMUM_NUMBER_OF_MATCHS) {
        return;
      }

      for(SavedMatch matchToAnalyse : matchsToAnalyse) {
        RoleMatchAnalysisWorker roleAnalysisWorker = new RoleMatchAnalysisWorker(matchToAnalyse, this);

        ServerThreadsManager.getDataAnalysisThread().execute(roleAnalysisWorker);
      }

      while(nbrMatchsToAnaylse != analysisDone.get()){
        TimeUnit.MILLISECONDS.sleep(500);
      }

      if(nbrMatch.get() == 0) {
        return;
      }

      double ratioTop = ((double) nbrTop.get() / nbrMatch.get()) * 100.0;
      double ratioJng = ((double) nbrJng.get() / nbrMatch.get()) * 100.0;
      double ratioMid = ((double) nbrMid.get() / nbrMatch.get()) * 100.0;
      double ratioAdc = ((double) nbrAdc.get() / nbrMatch.get()) * 100.0;
      double ratioSup = ((double) nbrSup.get() / nbrMatch.get()) * 100.0;

      StringBuilder rolesList = new StringBuilder();
      StringBuilder rolesStatsList = new StringBuilder();

      getRolesString(ratioTop, ratioJng, ratioMid, ratioAdc, ratioSup, rolesList, rolesStatsList);

      DTO.ChampionRoleAnalysis champRole = ChampionRoleAnalysisRepository.getChampionRoleAnalysis(championId);

      double averageKDA;
      if(deaths.get() == 0) {
        averageKDA = DangerosityReportKDA.DEFAULT_AVERAGE_KDA; //Impossible with a huge sample size to have 0 death, we put a basic value of 2.5 if this happens.
      }else {
        averageKDA = (kills.get() + assists.get()) / (double) deaths.get();
      }

      if(champion != null) {
        logger.info("Average KDA for {} determined as {}", champion.getName(), averageKDA);
      }

      if(champRole == null) {
        ChampionRoleAnalysisRepository.createChampionRoles(championId, rolesList.toString(), rolesStatsList.toString(), averageKDA);
      }else {
        ChampionRoleAnalysisRepository.updateChampionsRoles(championId, rolesList.toString(), rolesStatsList.toString(), averageKDA);
      }

      if(champion != null) {
        List<ChampionRole> championsRoles = new ArrayList<>();

        String[] roles = rolesList.toString().split(";");
        for(String strRole : roles) {
          if(strRole.isEmpty()) {
            continue;
          }
          ChampionRole role = ChampionRole.valueOf(strRole);

          if(role != null) {
            championsRoles.add(role);
          }
        }
        champion.setRoles(championsRoles);
        champion.setAverageKDA(averageKDA);
      }

    } catch(SQLException e) {
      logger.error("SQL Error with a query", e);
    } catch (InterruptedException e) {
      logger.error("interupted exception !", e);
      Thread.currentThread().interrupt();
    }

  }

  private String getRolesString(double ratioTop, double ratioJng, double ratioMid, double ratioAdc, double ratioSup,
      StringBuilder rolesListBuilder, StringBuilder rolesStatsListBuilder) {

    if(ratioTop > MINIMUM_POURCENTAGE_ROLE_COMMON) {
      rolesListBuilder.append(ChampionRole.TOP.toString() + ";");
      rolesStatsListBuilder.append(STATS_FORMAT.format(ratioTop) + ";");
      logger.info("{} detected as playable in top. Ratio of this role : {}%", champion.getName(), ratioTop);
    }

    if(ratioJng > MINIMUM_POURCENTAGE_ROLE_COMMON) {
      rolesListBuilder.append(ChampionRole.JUNGLE.toString() + ";");
      rolesStatsListBuilder.append(STATS_FORMAT.format(ratioJng) + ";");
      logger.info("{} detected as playable in jng. Ratio of this role : {}%", champion.getName(), ratioJng);
    }

    if(ratioMid > MINIMUM_POURCENTAGE_ROLE_COMMON) {
      rolesListBuilder.append(ChampionRole.MID.toString() + ";");
      rolesStatsListBuilder.append(STATS_FORMAT.format(ratioMid) + ";");
      logger.info("{} detected as playable in mid. Ratio of this role : {}%", champion.getName(), ratioMid);
    }

    if(ratioAdc > MINIMUM_POURCENTAGE_ROLE_COMMON) {
      rolesListBuilder.append(ChampionRole.ADC.toString() + ";");
      rolesStatsListBuilder.append(STATS_FORMAT.format(ratioAdc) + ";");
      logger.info("{} detected as playable in adc. Ratio of this role : {}%", champion.getName(), ratioAdc);
    }

    if(ratioSup > MINIMUM_POURCENTAGE_ROLE_COMMON) {
      rolesListBuilder.append(ChampionRole.SUPPORT.toString() + ";");
      rolesStatsListBuilder.append(STATS_FORMAT.format(ratioSup) + ";");
      logger.info("{} detected as playable in sup. Ratio of this role : {}%", champion.getName(), ratioSup);
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

  public AtomicInteger getKills() {
    return kills;
  }

  public AtomicInteger getDeaths() {
    return deaths;
  }

  public AtomicInteger getAssists() {
    return assists;
  }

}
