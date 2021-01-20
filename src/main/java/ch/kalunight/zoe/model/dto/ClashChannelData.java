package ch.kalunight.zoe.model.dto;

import java.util.ArrayList;
import java.util.List;

import net.rithms.riot.constant.Platform;

public class ClashChannelData {

  private List<Long> infoMessagesId;
  private List<Long> enemyTeamMessages;
  private Long gameCardId;
  private Long gameId;
  private Platform selectedPlatform;
  private String selectedSummonerId;
  private ClashStatus clashStatus;

  public ClashChannelData(List<Long> infoMessagesId, List<Long> teamsSpecificInfo, Long gameCardId, Platform selectedPlatform, String selectedSummonerId, ClashStatus clashStatus) {
    this.infoMessagesId = infoMessagesId;
    this.enemyTeamMessages = teamsSpecificInfo;
    this.gameCardId = gameCardId;
    this.selectedPlatform = selectedPlatform;
    this.selectedSummonerId = selectedSummonerId;
    this.clashStatus = clashStatus;
    this.gameId = null;
  }

  public List<Long> getAllClashChannel() {
    List<Long> cumuledAllMessage = new ArrayList<>();

    cumuledAllMessage.addAll(infoMessagesId);
    cumuledAllMessage.addAll(enemyTeamMessages);
    if(gameCardId != null) {
      cumuledAllMessage.add(gameCardId);
    }
    return cumuledAllMessage;
  }
  
  public Long getGameId() {
    return gameId;
  }

  public void setGameId(Long gameId) {
    this.gameId = gameId;
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

  public Platform getSelectedPlatform() {
    return selectedPlatform;
  }

  public void setSelectedPlatform(Platform selectedPlatform) {
    this.selectedPlatform = selectedPlatform;
  }

  public String getSelectedSummonerId() {
    return selectedSummonerId;
  }

  public void setSelectedSummonerId(String selectedSummonerId) {
    this.selectedSummonerId = selectedSummonerId;
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
