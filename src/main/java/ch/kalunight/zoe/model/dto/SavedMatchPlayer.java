package ch.kalunight.zoe.model.dto;

import java.io.Serializable;

import net.rithms.riot.api.endpoints.match.v5.dto.MatchParticipant;

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
  private String role;
  private String lane;
  
  public SavedMatchPlayer(boolean blueSide, String summonerId, int championId, MatchParticipant participant, String role, String lane) {
    this.blueSide = blueSide;
    this.summonerId = summonerId;
    this.championId = championId;
    this.kills = participant.getKills();
    this.deaths = participant.getDeaths();
    this.assists = participant.getAssists();
    this.creepScores = participant.getTotalMinionsKilled() + participant.getNeutralMinionsKilled();
    this.level = participant.getChampLevel();
    this.role = role;
    this.lane = lane;
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

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getLane() {
    return lane;
  }

  public void setLane(String lane) {
    this.lane = lane;
  }
  
}