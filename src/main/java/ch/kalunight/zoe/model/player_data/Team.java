package ch.kalunight.zoe.model.player_data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.kalunight.zoe.model.dto.DTO;

public class Team {

  private String name;
  private List<DTO.Player> players;

  public Team(String name, List<DTO.Player> players) {
    this.name = name;
    this.players = Collections.synchronizedList(players);
  }

  public Team(String name) {
    this.name = name;
    players = new ArrayList<>();
  }

  public boolean isPlayerInTheTeam(DTO.Player playerToCheck) {
    for(DTO.Player player : players) {
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

  public List<DTO.Player> getPlayers() {
    return players;
  }

  public void setPlayers(List<DTO.Player> players) {
    this.players = players;
  }
}
