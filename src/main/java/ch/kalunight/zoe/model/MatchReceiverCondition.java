package ch.kalunight.zoe.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;

import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;

public class MatchReceiverCondition {

  /**
   * Champion ID Data
   */
  private Integer championIdWanted;
  private String summonerIdLinkedToChampionId;
  
  private Set<Integer> queueIdSelected;
  
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  
  public MatchReceiverCondition(Integer championIdWanted, String summonerIdLinkedToChampion, LocalDateTime startDate, LocalDateTime endDate) {
    this.championIdWanted = championIdWanted;
    this.summonerIdLinkedToChampionId = summonerIdLinkedToChampion;
    this.startDate = startDate;
    this.endDate = endDate;
  }
  
  public MatchReceiverCondition(Set<Integer> queueIdSelected, LocalDateTime startDate, LocalDateTime endDate) {
    this.queueIdSelected = queueIdSelected;
    this.startDate = startDate;
    this.endDate = endDate;
  }
  
  public boolean isGivenMatchWanted(SavedMatch match) {
    if(championIdWanted != null) {
      SavedMatchPlayer playerToCheck = match.getSavedMatchPlayerBySummonerId(summonerIdLinkedToChampionId);
      if(championIdWanted != playerToCheck.getChampionId()) {
        return false;
      }
    }
    
    if(queueIdSelected != null && !queueIdSelected.contains(match.getQueueId())) {
      return false;
    }
    
    LocalDateTime matchStart = new Timestamp(match.getGameCreation()).toLocalDateTime();
    
    if(startDate != null && startDate.isBefore(matchStart)) {
      return false;
    }
    
    if(endDate != null && endDate.isAfter(matchStart)) {
      return false;
    }
    
    return true;
  }

  public Integer getChampionIdWanted() {
    return championIdWanted;
  }

  public void setChampionIdWanted(Integer championIdWanted) {
    this.championIdWanted = championIdWanted;
  }

  public LocalDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDateTime startDate) {
    this.startDate = startDate;
  }

  public LocalDateTime getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDateTime endDate) {
    this.endDate = endDate;
  }

  public String getSummonerIdLinkedToChampionId() {
    return summonerIdLinkedToChampionId;
  }

  public void setSummonerIdLinkedToChampionId(String summonerIdLinkedToChampionId) {
    this.summonerIdLinkedToChampionId = summonerIdLinkedToChampionId;
  }
  
}