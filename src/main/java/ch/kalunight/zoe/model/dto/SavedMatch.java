package ch.kalunight.zoe.model.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.exception.PlayerNotFoundException;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.match.dto.ParticipantIdentity;
import net.rithms.riot.api.endpoints.match.dto.ParticipantTimeline;
import net.rithms.riot.api.endpoints.match.dto.Player;

public class SavedMatch implements Serializable {

  private static final int BLUE_TEAM_ID = 100;

  private static final long serialVersionUID = -3423117740284389063L;
  
  private List<SavedMatchPlayer> accounts;

  private boolean blueSideHasWin;
  
  public SavedMatch(Match match) {
    accounts = new ArrayList<>();
    
    for(Participant participant : match.getParticipants()) {
      buildPlayer(match, participant);
    }
    
    if(match.getTeamByTeamId(100).getWin().equals("Win")) {
      blueSideHasWin = true;
    }else {
      blueSideHasWin = false;
    }
  }

  private void buildPlayer(Match match, Participant participant) {
    Player player = null;

    for(ParticipantIdentity participantIdentity : match.getParticipantIdentities()) {
      if(participant.getParticipantId() == participantIdentity.getParticipantId()) {
        player = participantIdentity.getPlayer();
      }
    }

    if(player != null) {
      ParticipantTimeline timeline = participant.getTimeline();
      String role = null;
      String lane = null;
      if(timeline != null) {
        role = timeline.getRole();
        lane = timeline.getLane();
      }
      
      boolean blueSide;
      if(participant.getTeamId() == BLUE_TEAM_ID) {
        blueSide = true;
      }else {
        blueSide = false;
      }
      
      SavedMatchPlayer savedPlayer = new SavedMatchPlayer(blueSide, player.getAccountId(), participant.getChampionId(), participant.getStats(), role, lane);

      accounts.add(savedPlayer);
    }
  }
  
  public SavedMatchPlayer getSavedMatchPlayerByAccountId(String accountId) {
    
    for(SavedMatchPlayer savedMatchPlayer : accounts) {
      if(savedMatchPlayer.getAccountId().equals(accountId)) {
        return savedMatchPlayer;
      }
    }
    return null;
  }
  
  public boolean isGivenAccountWinner(String accountId) {
    
    for(SavedMatchPlayer player : accounts) {
      if(player.getAccountId().equals(accountId)) {
        return (player.isBlueSide() && blueSideHasWin) || (!player.isBlueSide() && !blueSideHasWin);
      }
    }
    
    throw new PlayerNotFoundException("Impossible to give a winner in the game since the player is not in the game");
  }

  public List<SavedMatchPlayer> getAccounts() {
    return accounts;
  }

  public boolean isBlueSideHasWin() {
    return blueSideHasWin;
  }

  public void setBlueSideHasWin(boolean blueSideAsWin) {
    this.blueSideHasWin = blueSideAsWin;
  }

}
