package ch.kalunight.zoe.command.remove;

import java.sql.SQLException;
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

public class RemovePlayerToTeamCommand extends ZoeCommand {

  public static final String USAGE_NAME = "playerToTeam";

  public RemovePlayerToTeamCommand() {
    this.name = USAGE_NAME;
    this.help = "removePlayerToTeamHelpMessage";
    this.arguments = "@MentionOfPlayer";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(RemoveCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    event.getTextChannel().sendTyping().complete();

    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    if(event.getMessage().getMentionedMembers().size() != 1) {
      event.reply(LanguageManager.getText(server.getLanguage(), "removePlayerToTeamMissingMention"));
      return;
    }

    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, event.getMessage()
        .getMentionedMembers().get(0).getUser().getIdLong());

    if(player == null) {
      event.reply(LanguageManager.getText(server.getLanguage(), "removePlayerToTeamMentionnedPlayerNotPlayer"));
      return;
    }

    DTO.Team teamWhereRemove = TeamRepository.getTeamByPlayerAndGuild(server.serv_guildId, player.player_discordId);
    
    if(teamWhereRemove == null) {
      event.reply(LanguageManager.getText(server.getLanguage(), "removePlayerToTeamNotInTheTeam"));
      return;
    }

    PlayerRepository.updateTeamOfPlayerDefineNull(player.player_id);
    event.reply(String.format(LanguageManager.getText(server.getLanguage(), "removePlayerToTeamDoneMessage"),
        event.getJDA().retrieveUserById(player.player_discordId).complete().getName(), teamWhereRemove.team_name));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
