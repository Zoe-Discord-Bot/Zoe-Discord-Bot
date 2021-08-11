package ch.kalunight.zoe.model.clash;

import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.model.dangerosityreport.DangerosityReport;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportKDA;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportType;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;
import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
import no.stelar7.api.r4j.pojo.lol.championmastery.ChampionMastery;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

public class DataPerChampion implements Comparable<DataPerChampion> {

  private int championId;

  private List<LOLMatch> matchs;
  
  private Integer nbrWin;

  private Integer nbrLose;

  private Double winrate;

  private ChampionMastery mastery;
  
  private Double averageKDA;
  
  private List<DangerosityReport> dangerosityReports;
  
  public DataPerChampion(int championId, List<LOLMatch> matchs) {
    this.championId = championId;
    this.matchs = matchs;
    this.dangerosityReports = new ArrayList<>();
  }

  public double getWinrate() {
    if(winrate == null) {
      nbrWin = 0;
      nbrLose = 0;

      for(LOLMatch match : matchs) {
        for(MatchParticipant player : match.getParticipants()) {
          if(player.getChampionId() == championId) {
            if(player.didWin()){
              nbrWin++;
            }else {
              nbrLose++;
            }
          }
        }
      }

      if(getNumberOfGame() != 0) {
        winrate = (nbrWin.doubleValue() / (nbrWin + nbrLose)) * 100;
      }else {
        winrate = 0d;
      }
    }
    
    return winrate;
  }
  
  public double getAverageKDA() {
    if(averageKDA == null) {
      int kills = 0;
      int deaths = 0;
      int assists = 0;
      
      for(LOLMatch match : matchs) {
        MatchParticipant matchPlayer = null;
        
        for(MatchParticipant participant : match.getParticipants()) {
          if(participant.getChampionId() == championId) {
            matchPlayer = participant;
            break;
          }
        }
        
        if(matchPlayer != null) {
          kills += matchPlayer.getKills();
          deaths += matchPlayer.getDeaths();
          assists += matchPlayer.getAssists();
        }
      }
      if(deaths == 0) {
        averageKDA = DangerosityReportKDA.DEFAULT_AVERAGE_KDA; //Impossible with a huge sample size to have 0 death, we put a basic value of 2.5.
      }else {
        averageKDA = (kills + assists) / (double) deaths;
      }
    }
    return averageKDA;
  }
  
  @Override
  public int compareTo(DataPerChampion objectToTest) {
    if(objectToTest.getNumberOfGame() > getNumberOfGame()) {
      return 1;
    }else if(objectToTest.getNumberOfGame() < getNumberOfGame()) {
      return -1;
    }
    
    return 0;
  }
  
  public int getNumberOfWin() {
    if(nbrWin == null) {
      getWinrate();
    }
    
    return nbrWin;
  }
  
  public int getNumberOfLose() {
    if(nbrLose == null) {
      getWinrate();
    }
    
    return nbrLose;
  }
  
  public List<DangerosityReport> getDangerosityReports() {
    return dangerosityReports;
  }
  
  public DangerosityReport getDangerosityReport(DangerosityReportType type) {
    for(DangerosityReport report : dangerosityReports) {
      if(report.getReportType() == type) {
        return report;
      }
    }
    return null;
  }
  
  public int getNumberOfGame() {
    if(nbrWin == null || nbrLose == null) {
      return matchs.size();
    }else {
      return nbrLose + nbrWin;
    }
  }

  public int getChampionId() {
    return championId;
  }

  public List<LOLMatch> getMatchs() {
    return matchs;
  }

  public ChampionMastery getMastery() {
    return mastery;
  }

  public void setMastery(ChampionMastery mastery) {
    this.mastery = mastery;
  }
  
}
