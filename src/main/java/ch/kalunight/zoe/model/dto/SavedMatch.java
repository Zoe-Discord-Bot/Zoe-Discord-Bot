package ch.kalunight.zoe.model.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.exception.PlayerNotFoundException;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchTeam;

public class SavedMatch implements Serializable {

  private static final int BLUE_TEAM_ID = 100;

  private static final long serialVersionUID = -3423117740284389063L;

  private String platform;
  
  private String gameId;
  
  private List<SavedMatchPlayer> players;

  private GameQueueType queueId;

  private String gameVersion;

  private long gameCreation;
   
  /**
   * Match duration in seconds.
   */
  private long gameDurations;

  private boolean blueSideHasWin;
  
  public SavedMatch(LOLMatch match, String gameId, ZoePlatform platform) {
    this.platform = platform.getDbName();
    this.gameId = gameId;
    queueId = match.getQueueId();
    gameVersion = match.getGameVersion();
    gameDurations = match.getGameDuration();
    gameCreation = match.getGameCreation();
    
    players = new ArrayList<>();

    for(MatchParticipant participant : match.getParticipants()) {
      buildPlayer(participant);
    }

    MatchTeam blueTeam = null;
    for(MatchTeam team : match.getTeams()) {
      if(team.getTeamId().getValue() == 100) {
        blueTeam = team;
        break;
      }
    }
    
    if(blueTeam != null && blueTeam.didWin()) {
      blueSideHasWin = true;
    }else {
      blueSideHasWin = false;
    }
  }

  private void buildPlayer(MatchParticipant participant) {
    if(participant != null) {

      boolean blueSide;
      if(participant.getTeamId().getValue() == BLUE_TEAM_ID) {
        blueSide = true;
      }else {
        blueSide = false;
      }

      SavedMatchPlayer savedPlayer = new SavedMatchPlayer(blueSide, participant.getSummonerId(), participant.getChampionId(),
          participant);

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
  
  public ZoePlatform getZoePlatform() {
    return ZoePlatform.getZoePlatformByName(platform);
  }
  
  public String getGameId() {
    return gameId;
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

  public GameQueueType getQueueId() {
    return queueId;
  }

  public String getGameVersion() {
    return gameVersion;
  }
  
}