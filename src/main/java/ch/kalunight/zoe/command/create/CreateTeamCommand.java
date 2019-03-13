package ch.kalunight.zoe.command.create;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import ch.kalunight.zoe.model.Team;
import net.dv8tion.jda.core.Permission;

public class CreateTeamCommand extends Command {

  public CreateTeamCommand() {
    this.name = "team";
    this.arguments = "nameOfTheTeam";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Create a new team";
  }
  
  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    String nameTeam = event.getArgs();
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(server == null) {
      server = new Server(event.getGuild(), SpellingLangage.EN);
      ServerData.getServers().put(event.getGuild().getId(), server);
    }
    
    if(nameTeam.equals("")) {
      event.reply("Please give me a team name. (E.g. : `>create team Fnatic`)");
    }else {
      Team team = server.getTeamByName(nameTeam);
      
      if(team != null) {
        event.reply("A team with the given name already exist !");
      }else {
        server.getTeams().add(new Team(event.getArgs()));
        event.reply("The team \"" + event.getArgs() + "\" has been created !");
      }
    }
    
  }

}
