package ch.kalunight.zoe.model;

import java.util.ArrayList;
import java.util.List;

public class Team {

  private String name;
  private List<Player> players;

  public Team(String name, List<Player> players) {
    this.name = name;
    this.players = players;
  }

  public Team(String name) {
    this.name = name;
    players = new ArrayList<>();
  }

  public boolean isPlayerInTheTeam(Player playerToCheck) {
    for(Player player : players) {
      if(player.equals(playerToCheck)) {
        return true;
      }
    }
    return false;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Player> getPlayers() {
    return players;
  }

  public void setPlayers(List<Player> players) {
    this.players = players;
  }
}
