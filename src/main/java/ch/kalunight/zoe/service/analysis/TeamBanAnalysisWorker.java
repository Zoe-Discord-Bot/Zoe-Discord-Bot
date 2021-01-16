package ch.kalunight.zoe.service.analysis;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.clash.ClashTeamRegistration;
import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReport;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportSource;
import ch.kalunight.zoe.model.dangerosityreport.PickData;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.MessageManagerUtil;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.TeamUtil;
import net.dv8tion.jda.api.entities.TextChannel;

public class TeamBanAnalysisWorker implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(TeamBanAnalysisWorker.class);
  
  private static final List<DangerosityReportSource> ALL_SOURCES_LIST = Collections.synchronizedList(new ArrayList<>());

  private static final List<DangerosityReportSource> ONLY_CHAMPION_SOURCE_LIST = Collections.synchronizedList(new ArrayList<>());
  
  private Server server;

  private ClashChannel clashChannel;

  private TextChannel whereToSend;

  private ClashTeamRegistration clashTeam;

  private List<TeamPlayerAnalysisDataCollector> teamPlayersData;

  static {
    ALL_SOURCES_LIST.add(DangerosityReportSource.CHAMPION);
    ALL_SOURCES_LIST.add(DangerosityReportSource.PLAYER);
    
    ONLY_CHAMPION_SOURCE_LIST.add(DangerosityReportSource.CHAMPION);
  }
  
  public TeamBanAnalysisWorker(Server server, ClashChannel clashChannel,
      ClashTeamRegistration clashTeam, TextChannel whereToSend, List<TeamPlayerAnalysisDataCollector> teamPlayersData) {
    this.server = server;
    this.clashChannel = clashChannel;
    this.whereToSend = whereToSend;
    this.clashTeam = clashTeam;
    this.teamPlayersData = teamPlayersData;
  }

  @Override
  public void run() {

    try {
      StringBuilder messageBuilder = new StringBuilder();

      messageBuilder.append("**" + String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentTeamNameStats"), clashTeam.getTeam().getAbbreviation(),
          clashTeam.getTeam().getName(),
          clashTeam.getTeam().getTier().getTier()) + "**\n");

      TeamUtil.addPlayersStats(server, teamPlayersData, messageBuilder);

      TeamUtil.addFlexStats(server, teamPlayersData, messageBuilder);

      messageBuilder.append("\n");

      List<PickData> firstBans = TeamUtil.getHeighestDangerosityAllTeam(teamPlayersData, 3);

      messageBuilder.append(LanguageManager.getText(server.getLanguage(), "teamAnalysisBanRecomendationTitle") + "\n\n");

      addFirstBanPhase(messageBuilder, firstBans);

      messageBuilder.append("\n");

      Collections.sort(teamPlayersData);

      messageBuilder.append("\n");
      
      addSecondBanPhase(messageBuilder, firstBans);

      List<Long> messagesId = new ArrayList<>();
      if(clashChannel != null) {
        messagesId = clashChannel.clashChannel_data.getEnemyTeamMessages();
      }

      MessageManagerUtil.editOrCreateTheseMessages(messagesId, whereToSend, messageBuilder.toString());

      if(clashChannel != null) {
        ClashChannelRepository.updateClashChannel(clashChannel.clashChannel_data, clashChannel.clashChannel_channelId);
      }

    }catch (SQLException e) {
      whereToSend.sendMessage(LanguageManager.getText(server.getLanguage(), "deleteLeaderboardErrorDatabase"));
    }catch(Exception e) {
      logger.error("Unexpected error in TeamBanAnalysisWorker", e);
      whereToSend.sendMessage(LanguageManager.getText(server.getLanguage(), "statsProfileUnexpectedError"));
    }

  }

  private void addSecondBanPhase(StringBuilder messageBuilder, List<PickData> firstBans) {
    messageBuilder.append("**" + LanguageManager.getText(server.getLanguage(), "teamAnalysisBanRecomendationSecondBanTitle") + "**\n");
    
    for(TeamPlayerAnalysisDataCollector player : teamPlayersData) {
      ChampionRole role = player.getFinalDeterminedPosition();

      List<PickData> playerPick = player.getPicksCompiledData();
      playerPick.removeAll(firstBans);

      List<PickData> picksToShow = TeamUtil.getHeighestDangerosity(playerPick, 2);

      messageBuilder.append(LanguageManager.getText(server.getLanguage(), TeamUtil.getChampionRoleID(role)) + " " + player.getSummoner().getSumCacheData().getName());
      
      if(!picksToShow.isEmpty()) {
        
        List<DangerosityReport> reportSources = picksToShow.get(0).getDangerosityReportBySource(DangerosityReportSource.PLAYER);

        boolean firstReportSend = false;
        int numberOfReportToTreat = reportSources.size();
        
        for(DangerosityReport report : reportSources) {
            if(report.getReportValue() != DangerosityReport.BASE_SCORE) {
              if(firstReportSend) {
                messageBuilder.append(", ");
              }else {
                messageBuilder.append(" (");
              }
              
              messageBuilder.append(report.getInfoToShow(server.getLanguage()));
              firstReportSend = true;
            }
            
            numberOfReportToTreat--;
            if(numberOfReportToTreat == 0 && firstReportSend) {
              messageBuilder.append(")");
            }
        }
        
        messageBuilder.append(":\n");
      }else {
        messageBuilder.append(":\n  -> *" + LanguageManager.getText(server.getLanguage(), "empty") + "*");
      }
      
      int pickToTreat = picksToShow.size();
      for(PickData pick : picksToShow) {
        Champion championData = Ressources.getChampionDataById(pick.getChampionId());

        String championString = LanguageManager.getText(server.getLanguage(), "unknown");
        if(championData != null) {
          championString = championData.getEmoteUsable() + " " + championData.getName();
        }

        showOneChampion(messageBuilder, pick, championString, ONLY_CHAMPION_SOURCE_LIST);
        pickToTreat--;
        if(pickToTreat != 0) {
          messageBuilder.append("\n");
        }
      }
      messageBuilder.append("\n");
    }
  }

  private void showOneChampion(StringBuilder messageBuilder, PickData pick, String championString, List<DangerosityReportSource> sourceToShow) {
    messageBuilder.append("  -> " + championString);

    boolean firstReportSend = false;
    
    int numberOfReportToTreat = pick.getReportsOfThePick().size();
    for(DangerosityReport report : pick.getReportsOfThePick()) {
      
      if(report.getReportValue() != DangerosityReport.BASE_SCORE && sourceToShow.contains(report.getReportSource())) {
        
        if(firstReportSend) {
          messageBuilder.append(", ");
        }else {
          messageBuilder.append(" (");
        }
        
        messageBuilder.append(report.getInfoToShow(server.getLanguage()));
        
        firstReportSend = true;
      }
      
      numberOfReportToTreat--;
      if(numberOfReportToTreat == 0 && firstReportSend) {
        messageBuilder.append(")");
      }
    }
  }

  private void addFirstBanPhase(StringBuilder messageBuilder, List<PickData> firstBans) {
    messageBuilder.append("**" + LanguageManager.getText(server.getLanguage(), "teamAnalysisBanRecomendationFirstBanTitle") + "**");

    for(PickData pickData : firstBans) {

      Champion championData = Ressources.getChampionDataById(pickData.getChampionId());

      String championString = LanguageManager.getText(server.getLanguage(), "unknown");
      if(championData != null) {
        championString = championData.getEmoteUsable() + " " + championData.getName();
      }

      messageBuilder.append("\n");
      showOneChampion(messageBuilder, pickData, championString, ALL_SOURCES_LIST);
      
    }
  }

}
