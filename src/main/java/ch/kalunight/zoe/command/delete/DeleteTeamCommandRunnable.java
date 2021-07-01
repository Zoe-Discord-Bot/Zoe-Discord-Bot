package ch.kalunight.zoe.command.delete;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.TeamRepository;
import ch.kalunight.zoe.translation.LanguageManager;

public class DeleteTeamCommandRunnable {

  public static final String USAGE_NAME = "team";

  private DeleteTeamCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(Server server, String teamName) throws SQLException {
    
    DTO.Team team = TeamRepository.getTeam(server.serv_guildId, teamName);
    if(team == null) {
      return String.format(LanguageManager.getText(server.getLanguage(), "deleteTeamNotFound"), teamName);
    } else {
      List<DTO.Player> players = PlayerRepository.getPlayers(server.serv_guildId);
      List<Long> playersIdInTheTeam = new ArrayList<>();
      for(DTO.Player player : players) {
        if(player.player_fk_team == team.team_id) {
          playersIdInTheTeam.add(player.player_id);
        }
      }
      
      TeamRepository.deleteTeam(team.team_id, playersIdInTheTeam);
      return String.format(LanguageManager.getText(server.getLanguage(), "deleteTeamDoneMessage"), teamName);
    }
  }
}
