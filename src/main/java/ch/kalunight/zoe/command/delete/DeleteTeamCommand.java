package ch.kalunight.zoe.command.delete;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.TeamRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class DeleteTeamCommand extends ZoeCommand {

  public static final String USAGE_NAME = "team";

  public DeleteTeamCommand() {
    this.name = USAGE_NAME;
    this.help = "deleteTeamHelpMessage";
    this.arguments = "teamName";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeletePlayerCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    event.getTextChannel().sendTyping().complete();
    
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    String teamName = event.getArgs();
    
    DTO.Team team = TeamRepository.getTeam(server.serv_guildId, teamName);
    if(team == null) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "deleteTeamNotFound"), teamName));
    } else {
      List<DTO.Player> players = PlayerRepository.getPlayers(server.serv_guildId);
      List<Long> playersIdInTheTeam = new ArrayList<>();
      for(DTO.Player player : players) {
        if(player.player_fk_team == team.team_id) {
          playersIdInTheTeam.add(player.player_id);
        }
      }
      
      TeamRepository.deleteTeam(team.team_id, playersIdInTheTeam);
      event.reply(String.format(LanguageManager.getText(server.serv_language, "deleteTeamDoneMessage"), teamName));
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
