package ch.kalunight.zoe.model.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.kalunight.zoe.exception.PlayerNotFoundException;
import ch.kalunight.zoe.util.MongodbDateUtil;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchTeam;

public class SavedMatch implements Serializable {

  private static final int BLUE_TEAM_ID = 100;

  private static final long serialVersionUID = -3423117740284389063L;

  private ZoePlatform platform;
  
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

  private Date retrieveDate;
  
  public SavedMatch() {}
  
  public SavedMatch(LOLMatch match, String gameId, ZoePlatform platform) {
    this.platform = platform;
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
    
    this.retrieveDate = MongodbDateUtil.toDate(LocalDateTime.now());
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

  public ZoePlatform getPlatform() {
    return platform;
  }

  public void setPlatform(ZoePlatform platform) {
    this.platform = platform;
  }

  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

  public List<SavedMatchPlayer> getPlayers() {
    return players;
  }

  public void setPlayers(List<SavedMatchPlayer> players) {
    this.players = players;
  }

  public GameQueueType getQueueId() {
    return queueId;
  }

  public void setQueueId(GameQueueType queueId) {
    this.queueId = queueId;
  }

  public String getGameVersion() {
    return gameVersion;
  }

  public void setGameVersion(String gameVersion) {
    this.gameVersion = gameVersion;
  }

  public long getGameCreation() {
    return gameCreation;
  }

  public void setGameCreation(long gameCreation) {
    this.gameCreation = gameCreation;
  }

  public long getGameDurations() {
    return gameDurations;
  }

  public void setGameDurations(long gameDurations) {
    this.gameDurations = gameDurations;
  }

  public boolean isBlueSideHasWin() {
    return blueSideHasWin;
  }

  public void setBlueSideHasWin(boolean blueSideHasWin) {
    this.blueSideHasWin = blueSideHasWin;
  }

  public Date getRetrieveDate() {
    return retrieveDate;
  }

  public void setRetrieveDate(Date retrieveDate) {
    this.retrieveDate = retrieveDate;
  }

  public static int getBlueTeamId() {
    return BLUE_TEAM_ID;
  }

  public static long getSerialversionuid() {
    return serialVersionUID;
  }
  
}