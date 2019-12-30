package ch.kalunight.zoe.model.dto;

import java.util.ArrayList;
import java.util.List;

import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.match.dto.ParticipantIdentity;
import net.rithms.riot.api.endpoints.match.dto.Player;

public class SavedMatch {

  private List<SavedMatchPlayer> accountsIdBlueSide;
  private List<SavedMatchPlayer> accountsIdRedSide;

  private boolean blueSideHasWin;
  
  public SavedMatch(Match match) {
    accountsIdBlueSide = new ArrayList<>();
    accountsIdRedSide = new ArrayList<>();
    
    for(Participant participant : match.getParticipants()) {

      Player player = null;

      for(ParticipantIdentity participantIdentity : match.getParticipantIdentities()) {
        if(participant.getParticipantId() == participantIdentity.getParticipantId()) {
          player = participantIdentity.getPlayer();
        }
      }

      if(player != null) {
        SavedMatchPlayer savedPlayer = new SavedMatchPlayer(player.getAccountId(), participant.getChampionId());

        if(participant.getTeamId() == 100) {
          accountsIdBlueSide.add(savedPlayer);
        }else {
          accountsIdRedSide.add(savedPlayer);
        }
      }
    }
    
    if(match.getTeamByTeamId(100).getWin().equals("Win")) {
      blueSideHasWin = true;
    }else {
      blueSideHasWin = false;
    }
  }
  
  public boolean isGivenAccountWinner(String accountId) {
    boolean playerBlueSide = false;
    
    for(SavedMatchPlayer player : accountsIdBlueSide) {
      if(player.getAccountId().equals(accountId)) {
        playerBlueSide = true;
        break;
      }
    }
    
    return playerBlueSide && blueSideHasWin;
  }
  
  public List<SavedMatchPlayer> getAccountsIdBlueSide() {
    return accountsIdBlueSide;
  }

  public void setAccountsIdBlueSide(List<SavedMatchPlayer> accountsIdBlueSide) {
    this.accountsIdBlueSide = accountsIdBlueSide;
  }

  public List<SavedMatchPlayer> getAccountsIdRedSide() {
    return accountsIdRedSide;
  }

  public void setAccountsIdRedSide(List<SavedMatchPlayer> accountsIdRedSide) {
    this.accountsIdRedSide = accountsIdRedSide;
  }

  public boolean isBlueSideHasWin() {
    return blueSideHasWin;
  }

  public void setBlueSideHasWin(boolean blueSideAsWin) {
    this.blueSideHasWin = blueSideAsWin;
  }

}
