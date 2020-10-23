package ch.kalunight.zoe.model.dto;

import java.util.List;

public class ClashTeamMessageManager {

  private List<Long> infoMessagesId;
  private List<ClashTeamInfo> teamsSpecificInfo;
  private Long selectedLeagueAccount;
  
  public ClashTeamMessageManager(List<Long> infoMessagesId, List<ClashTeamInfo> teamsSpecificInfo, Long selectedLeagueAccount) {
    this.infoMessagesId = infoMessagesId;
    this.teamsSpecificInfo = teamsSpecificInfo;
    this.selectedLeagueAccount = selectedLeagueAccount;
  }

  public List<Long> getInfoMessagesId() {
    return infoMessagesId;
  }

  public void setInfoMessagesId(List<Long> infoMessagesId) {
    this.infoMessagesId = infoMessagesId;
  }

  public List<ClashTeamInfo> getTeamsSpecificInfo() {
    return teamsSpecificInfo;
  }

  public void setTeamsSpecificInfo(List<ClashTeamInfo> teamsSpecificInfo) {
    this.teamsSpecificInfo = teamsSpecificInfo;
  }

  public Long getSelectedLeagueAccountId() {
    return selectedLeagueAccount;
  }

  public void setSelectedLeagueAccountId(Long selectedLeagueAccountId) {
    this.selectedLeagueAccount = selectedLeagueAccountId;
  }
}
