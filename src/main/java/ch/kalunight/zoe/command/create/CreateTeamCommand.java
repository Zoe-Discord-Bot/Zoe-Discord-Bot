package ch.kalunight.zoe.command.create;

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

public class CreateTeamCommand extends ZoeCommand {

  public static final String USAGE_NAME = "team";

  public CreateTeamCommand() {
    this.name = USAGE_NAME;
    this.arguments = "nameOfTheTeam";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "createTeamHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    String nameTeam = event.getArgs();
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(nameTeam.equals("--server")) {
      event.reply(LanguageManager.getText(server.getLangage(), "nameAlreadyUsedByTheSystem"));
      return;
    }

    if(nameTeam.equals("")) {
      event.reply(LanguageManager.getText(server.getLangage(), "createTeamNeedName"));
    } else {
      Team team = server.getTeamByName(nameTeam);

      if(team != null) {
        event.reply(LanguageManager.getText(server.getLangage(), "createTeamNameAlreadyExist"));
      } else {
        server.getTeams().add(new Team(event.getArgs()));
        event.reply(String.format(LanguageManager.getText(server.getLangage(), "createTeamDoneMessage"), event.getArgs()));
      }
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
