package ch.kalunight.zoe.model.dto;

import java.util.List;

public class ClashTeamInfo {

  private List<Long> teamMessagesInfoId;
  private Long gameCardId;
  
  public ClashTeamInfo(List<Long> teamMessagesInfoId, Long gameCardId) {
    this.teamMessagesInfoId = teamMessagesInfoId;
    this.gameCardId = gameCardId;
  }

  public List<Long> getTeamMessagesInfoId() {
    return teamMessagesInfoId;
  }

  public Long getGameCardId() {
    return gameCardId;
  }

  public void setTeamMessagesInfoId(List<Long> teamMessagesInfoId) {
    this.teamMessagesInfoId = teamMessagesInfoId;
  }

  public void setGameCardId(Long gameCardId) {
    this.gameCardId = gameCardId;
  }
}
