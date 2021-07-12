package ch.kalunight.zoe.command.remove;

import java.sql.SQLException;
import java.util.List;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.TeamRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class RemovePlayerToTeamCommandRunnable {

  public static final String USAGE_NAME = "playertoteam";
  
  private RemovePlayerToTeamCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(Server server, List<Member> mentionnedPlayers, JDA jda) throws SQLException {
    
    if(mentionnedPlayers.size() != 1) {
      return LanguageManager.getText(server.getLanguage(), "removePlayerToTeamMissingMention");
    }

    User user = mentionnedPlayers.get(0).getUser();
    
    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());

    if(player == null) {
      return LanguageManager.getText(server.getLanguage(), "removePlayerToTeamMentionnedPlayerNotPlayer");
    }

    DTO.Team teamWhereRemove = TeamRepository.getTeamByPlayerAndGuild(server.serv_guildId, player.player_discordId);
    
    if(teamWhereRemove == null) {
      return LanguageManager.getText(server.getLanguage(), "removePlayerToTeamNotInTheTeam");
    }

    PlayerRepository.updateTeamOfPlayerDefineNull(player.player_id);
    return String.format(LanguageManager.getText(server.getLanguage(), "removePlayerToTeamDoneMessage"), user.getName(), teamWhereRemove.team_name);
  }
}
