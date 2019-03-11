package ch.kalunight.zoe.model;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;
import org.testng.annotations.Test;
import net.rithms.riot.constant.Platform;

public class ServerTest {

  @Test
  public void getAllPlayersTeamGivenPlayersWithNoTeamShouldReturnPlayerWithOneTeam() {
    // GIVEN
    Server server = new Server(null, SpellingLangage.EN);
    Player player1 = new Player(null, null, Platform.EUW, false);
    Player player2 = new Player(null, null, Platform.EUW, false);
    server.getPlayers().add(player1);
    server.getPlayers().add(player2);
    
    List<Team> expectedTeamsList = new ArrayList<>();
    Team expectedTeam = new Team("No Team");
    List<Player> expectedPlayers = new ArrayList<Player>();
    expectedPlayers.add(player1);
    expectedPlayers.add(player2);
    expectedTeam.setPlayers(expectedPlayers);
    
    // WHEN
    List<Team> resultTeam = server.getAllPlayerTeams();
    
    // THEN
    assertEquals(expectedTeamsList.size(), resultTeam.size());
    assertEquals(expectedTeam.getName(), resultTeam.get(0).getName());
    assertEquals(expectedPlayers.size(), resultTeam.get(0).getPlayers().size());
  }
  
}
