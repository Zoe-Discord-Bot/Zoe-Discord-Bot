package ch.kalunight.zoe.command.add;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.TeamRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;

public class AddPlayerToTeamCommandRunnable {

  public static final String USAGE_NAME = "playertoteam";
  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");

  private AddPlayerToTeamCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(Server server, List<Member> mentionnedMembers, String args) throws SQLException {
    
    if(mentionnedMembers.size() != 1) {
      return LanguageManager.getText(server.getLanguage(), "mentionOfPlayerNeeded");
    } else {
      
      DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId,
          mentionnedMembers.get(0).getUser().getIdLong());
      
      if(player == null) {
        return LanguageManager.getText(server.getLanguage(), "mentionOfUserNeedToBeAPlayer");
      } else {

        DTO.Team team = TeamRepository.getTeamByPlayerAndGuild(server.serv_guildId, player.player_discordId);
        if(team != null) {
          return String.format(LanguageManager.getText(server.getLanguage(), "mentionnedPlayerIsAlreadyInATeam"), team.team_name);
        } else {
          Matcher matcher = PARENTHESES_PATTERN.matcher(args);
          String teamName = "";
          while(matcher.find()) {
            teamName = matcher.group(1);
          }

          DTO.Team teamToAdd = TeamRepository.getTeam(server.serv_guildId, teamName);
          if(teamToAdd == null) {
            return LanguageManager.getText(server.getLanguage(), "givenTeamNotExist");
          } else {
            PlayerRepository.updateTeamOfPlayer(player.player_id, teamToAdd.team_id);
            return LanguageManager.getText(server.getLanguage(), "playerAddedInTheTeam");
          }
        }
      }
    }
  }
}
