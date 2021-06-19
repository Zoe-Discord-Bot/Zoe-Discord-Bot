package ch.kalunight.zoe.model.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.exception.PlayerNotFoundException;
import net.rithms.riot.api.endpoints.match.v5.dto.MatchParticipant;
import net.rithms.riot.api.endpoints.match.v5.dto.MatchV5;

public class SavedMatch implements Serializable {

  private static final int BLUE_TEAM_ID = 100;

  private static final long serialVersionUID = -3423117740284389063L;

  private List<SavedMatchPlayer> players;

  private int queueId;

  private String gameVersion;

  private long gameCreation;
  
  /**
   * Match duration in seconds.
   */
  private long gameDurations;

  private boolean blueSideHasWin;

  public SavedMatch(MatchV5 match) {
    queueId = match.getInfo().getQueueId();
    gameVersion = match.getInfo().getGameVersion();
    gameDurations = match.getInfo().getGameDuration();
    gameCreation = match.getInfo().getGameCreation();
    
    players = new ArrayList<>();

    for(MatchParticipant participant : match.getInfo().getParticipants()) {
      buildPlayer(match, participant);
    }

    if(match.getInfo().getTeamByTeamId(100).isWin()) {
      blueSideHasWin = true;
    }else {
      blueSideHasWin = false;
    }
  }

  private void buildPlayer(MatchV5 match, MatchParticipant participant) {

    if(participant != null) {
      String role = participant.getRole();
      String lane = participant.getLane();

      boolean blueSide;
      if(participant.getTeamId() == BLUE_TEAM_ID) {
        blueSide = true;
      }else {
        blueSide = false;
      }

      SavedMatchPlayer savedPlayer = new SavedMatchPlayer(blueSide, participant.getSummonerId(), participant.getChampionId(),
          participant, role, lane);

      players.add(savedPlayer);
    }
  }

  public SavedMatchPlayer getSavedMatchPlayerBySummonerId(String summonerId) {

    for(SavedMatchPlayer savedMatchPlayer : players) {
      if(savedMatchPlayer.getSummonerId().equals(summonerId)) {
        return savedMatchPlayer;
      }
    }
    return null;
  }

  public SavedMatchPlayer getSavedMatchPlayerByChampionId(int championId) {

    for(SavedMatchPlayer savedMatchPlayer : players) {
      if(savedMatchPlayer.getChampionId() == championId) {
        return savedMatchPlayer;
      }
    }
    return null;
  }

  public boolean isGivenAccountWinner(String summonerId) {

    for(SavedMatchPlayer player : players) {
      if(player.getSummonerId().equals(summonerId)) {
        return (player.isBlueSide() && blueSideHasWin) || (!player.isBlueSide() && !blueSideHasWin);
      }
    }

    throw new PlayerNotFoundException("Impossible to give a winner in the game since the player is not in the game");
  }

  public long getGameCreation() {
    return gameCreation;
  }

  public long getGameDurations() {
    return gameDurations;
  }
  
  public List<SavedMatchPlayer> getPlayers() {
    return players;
  }

  public boolean isBlueSideHasWin() {
    return blueSideHasWin;
  }

  public int getQueueId() {
    return queueId;
  }

  public String getGameVersion() {
    return gameVersion;
  }

}
