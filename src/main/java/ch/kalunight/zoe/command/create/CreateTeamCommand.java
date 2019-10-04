package ch.kalunight.zoe.command.create;

import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.player_data.Team;
import ch.kalunight.zoe.model.static_data.SpellingLangage;
import net.dv8tion.jda.api.Permission;

public class CreateTeamCommand extends Command {

  public static final String USAGE_NAME = "team";

  public CreateTeamCommand() {
    this.name = USAGE_NAME;
    this.arguments = "nameOfTheTeam";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Create a new team. Allows you to group players together on the Info Panel. Manage Channel permission needed.";
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    String nameTeam = event.getArgs();
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(nameTeam.equals("--server")) {
      event.reply("This name is already used by the system. Please us another name.");
      return;
    }

    if(server == null) {
      server = new Server(event.getGuild(), SpellingLangage.EN, new ServerConfiguration());
      ServerData.getServers().put(event.getGuild().getId(), server);
    }

    if(nameTeam.equals("")) {
      event.reply("Please give me a team name. (E.g. : `>create team Fnatic`)");
    } else {
      Team team = server.getTeamByName(nameTeam);

      if(team != null) {
        event.reply("A team with the given name already exist !");
      } else {
        server.getTeams().add(new Team(event.getArgs()));
        event.reply("The team \"" + event.getArgs() + "\" has been created !");
      }
    }
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Create Team command :\n");
        stringBuilder.append("--> `>create " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
}
