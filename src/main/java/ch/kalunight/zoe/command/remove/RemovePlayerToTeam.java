package ch.kalunight.zoe.command.remove;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import net.dv8tion.jda.core.Permission;

public class RemovePlayerToTeam extends Command {

  public RemovePlayerToTeam() {
    this.name = "playerToTeam";
    this.help = "Delete the given player from the given Team";
    this.arguments = "*MentionOfPlayer* (*teamName*)";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
  }
  
  @Override
  protected void execute(CommandEvent event) {
    Server sever = ServerData.getServers().get(event.getGuild().getId());
    
    if(sever == null) {
      sever = new Server(event.getGuild(), SpellingLangage.EN);
      ServerData.getServers().put(event.getGuild().getId(), sever);
    }
    
    if(event.getMessage().getMentionedMembers().size() != 1) {
      event.reply("Please mentions one people !");
      return;
    }
    
    Player player = sever.getPlayerByDiscordId(event.getMessage().getMentionedMembers().get(0).getUser().getId());
    
  }
}
