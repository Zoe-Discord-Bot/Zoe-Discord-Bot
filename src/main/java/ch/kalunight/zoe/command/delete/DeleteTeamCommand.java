package ch.kalunight.zoe.command.delete;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import ch.kalunight.zoe.model.Team;
import net.dv8tion.jda.core.Permission;

public class DeleteTeamCommand extends Command {

  public DeleteTeamCommand() {
    this.name = "team";
    this.help = "Delete the given team";
    this.arguments = "*teamName*";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
  }
  
  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    String teamName = event.getArgs();
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(server == null) {
      server = new Server(event.getGuild(), SpellingLangage.EN);
      ServerData.getServers().put(event.getGuild().getId(), server);
    }
    
    Team team = server.getTeamByName(teamName);
    if(team == null) {
      event.reply("There is no team called \"" + teamName + "\" !");
    }else {
      server.getTeams().remove(team);
      event.reply("The team \"" + teamName + "\" has been deleted !");
    }
  }

}
