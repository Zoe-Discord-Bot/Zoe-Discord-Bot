package ch.kalunight.zoe.command.delete;

import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

public class DeletePlayerCommand extends Command{

  public DeletePlayerCommand() {
    this.name = "player";
    this.help = "Delete the given player";
    this.arguments = "*@DiscordMentionOfPlayer*";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
  }
  
  @Override
  protected void execute(CommandEvent event) {
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(server == null) {
      server = new Server(event.getGuild(), SpellingLangage.EN);
    }
    
    List<Member> members = event.getMessage().getMentionedMembers();
    
    if(members.size() != 1) {
      event.reply("You need to mention 1 people !");
    }else {
      User user = members.get(0).getUser();
      Player player = server.getPlayerByDiscordId(user.getId());
      
      if(player == null) {
        event.reply("This people is not registered !");
      }else{
        server.deletePlayer(player);
        event.reply("This people has been deleted !");
      }
    }
  }

}
