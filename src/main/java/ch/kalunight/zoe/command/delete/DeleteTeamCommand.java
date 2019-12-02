package ch.kalunight.zoe.command.delete;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.Team;
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
  protected void executeCommand(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    String teamName = event.getArgs();
    Server server = ServerData.getServers().get(event.getGuild().getId());

    Team team = server.getTeamByName(teamName);
    if(team == null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "deleteTeamNotFound"), teamName));
    } else {
      server.getTeams().remove(team);
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "deleteTeamDoneMessage"), teamName));
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
