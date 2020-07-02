package ch.kalunight.zoe.model.dto;

import java.io.Serializable;
import net.rithms.riot.api.endpoints.match.dto.ParticipantStats;

public class SavedMatchPlayer implements Serializable {

  private static final long serialVersionUID = 5432783425736075514L;
  
  private String accountId;
  private int championId;
  private int kills;
  private int deaths;
  private int assits;
  
  public SavedMatchPlayer(String accountId, int championId, ParticipantStats participantStats) {
    this.accountId = accountId;
    this.championId = championId;
    this.kills = participantStats.getKills();
    this.deaths = participantStats.getDeaths();
    this.assits = participantStats.getAssists();
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
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

  public int getAssits() {
    return assits;
  }
}