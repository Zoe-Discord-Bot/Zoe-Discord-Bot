package ch.kalunight.zoe.model.leaderboard.dataholder;

import ch.kalunight.zoe.model.dto.DTO.Player;

public class PlayerKDA {

  private Player player;
  private int kills;
  private int deaths;
  private int assists;
  
  public PlayerKDA(Player player, int kills, int deaths, int assists) {
    this.player = player;
    this.kills = kills;
    this.deaths = deaths;
    this.assists = assists;
  }

  public Player getPlayer() {
    return player;
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
  
}
