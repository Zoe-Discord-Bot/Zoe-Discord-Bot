package ch.kalunight.zoe.model.dto;

import java.io.Serializable;

import no.stelar7.api.r4j.basic.constants.types.lol.LaneType;
import no.stelar7.api.r4j.basic.constants.types.lol.RoleType;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;

public class SavedMatchPlayer implements Serializable {

  private static final long serialVersionUID = 5432783425736075514L;
  
  private boolean blueSide;
  private String summonerId;
  private int championId;
  private int kills;
  private int deaths;
  private int assists;
  private int creepScores;
  private int level;
  private RoleType role;
  private LaneType lane;
  
  public SavedMatchPlayer(boolean blueSide, String summonerId, int championId, MatchParticipant participantStats) {
    this.blueSide = blueSide;
    this.summonerId = summonerId;
    this.championId = championId;
    this.kills = participantStats.getKills();
    this.deaths = participantStats.getDeaths();
    this.assists = participantStats.getAssists();
    this.creepScores = participantStats.getTotalMinionsKilled() + participantStats.getNeutralMinionsKilled();
    this.level = participantStats.getChampLevel();
    this.role = participantStats.getRole();
    this.lane = participantStats.getLane();
  }

  public boolean didWin(SavedMatch originalMatch) {
    return originalMatch.isGivenAccountWinner(summonerId);
  }

  public int getCreepScores() {
    return creepScores;
  }

  public int getLevel() {
    return level;
  }

  public boolean isBlueSide() {
    return blueSide;
  }

  public void setBlueSide(boolean blueSide) {
    this.blueSide = blueSide;
  }
  
  public String getSummonerId() {
    return summonerId;
  }

  public void setSummonerId(String summonerId) {
    this.summonerId = summonerId;
  }

  public int getChampionId() {
    return championId;
  }

  public void setChampionId(int championId) {
    this.championId = championId;
  }

  public int getKills() {
    return kills;
  }

  public int getDeaths() {
    return deaths;
  }

  public int getAssists() {
    return assists;
  }

  public RoleType getRole() {
    return role;
  }

  public void setRole(RoleType role) {
    this.role = role;
  }

  public LaneType getLane() {
    return lane;
  }

  public void setLane(LaneType lane) {
    this.lane = lane;
  }
  
}
