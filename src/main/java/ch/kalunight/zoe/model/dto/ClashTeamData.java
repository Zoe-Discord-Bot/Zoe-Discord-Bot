package ch.kalunight.zoe.model.dto;

import java.util.List;

public class ClashTeamData {

  private List<Long> infoMessagesId;
  private List<Long> enemyTeamMessages;
  private Long gameCardId;
  private Long selectedLeagueAccount;
  private ClashStatus clashStatus;
  
  public ClashTeamData(List<Long> infoMessagesId, List<Long> teamsSpecificInfo, Long gameCardId, Long selectedLeagueAccount, ClashStatus clashStatus) {
    this.infoMessagesId = infoMessagesId;
    this.enemyTeamMessages = teamsSpecificInfo;
    this.gameCardId = gameCardId;
    this.selectedLeagueAccount = selectedLeagueAccount;
    this.clashStatus = clashStatus;
  }

  public List<Long> getInfoMessagesId() {
    return infoMessagesId;
  }

  public void setInfoMessagesId(List<Long> infoMessagesId) {
    this.infoMessagesId = infoMessagesId;
  }

  public List<Long> getEnemyTeamMessages() {
    return enemyTeamMessages;
  }

  public void setTeamsSpecificInfo(List<Long> teamsSpecificInfo) {
    this.enemyTeamMessages = teamsSpecificInfo;
  }

  public Long getSelectedLeagueAccountId() {
    return selectedLeagueAccount;
  }

  public void setSelectedLeagueAccountId(Long selectedLeagueAccountId) {
    this.selectedLeagueAccount = selectedLeagueAccountId;
  }

  public ClashStatus getClashStatus() {
    return clashStatus;
  }

  public void setClashStatus(ClashStatus clashStatus) {
    this.clashStatus = clashStatus;
  }

  public Long getGameCardId() {
    return gameCardId;
  }

  public void setGameCardId(Long gameCardId) {
    this.gameCardId = gameCardId;
  }
  
}
