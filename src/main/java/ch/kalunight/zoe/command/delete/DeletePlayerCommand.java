package ch.kalunight.zoe.command.delete;

import java.util.List;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.model.static_data.SpellingLangage;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class DeletePlayerCommand extends Command {

  public static final String USAGE_NAME = "player";

  public DeletePlayerCommand() {
    this.name = USAGE_NAME;
    this.help = "deletePlayerHelpMessage";
    this.arguments = "@DiscordMentionOfPlayer";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeleteCommand.USAGE_NAME, USAGE_NAME, arguments, help);
  }

  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(!server.getConfig().getUserSelfAdding().isOptionActivated() && 
        !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      
        event.reply(String.format(LanguageManager.getText(server.getLangage(), "deletePlayerMissingPermission"),
            Permission.MANAGE_CHANNEL.getName()));
        return;
    }
    
    List<Member> members = event.getMessage().getMentionedMembers();

    if(members.size() != 1) {
      event.reply(LanguageManager.getText(server.getLangage(), "deletePlayerMissingMentionPlayer"));
    } else {
      User user = members.get(0).getUser();
      if(!user.equals(event.getAuthor()) && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
        event.reply(String.format(LanguageManager.getText(server.getLangage(), "deletePlayerOtherPlayerMissingPermission"), 
            Permission.MANAGE_CHANNEL.getName()));
        return;
      }
      
      Player player = server.getPlayerByDiscordId(user.getId());
      
      if(player == null) {
        event.reply(user.getName() + " is not registered !");
      } else {
        server.deletePlayer(player);
        if(server.getConfig().getZoeRoleOption().getRole() != null) {
          Member member = server.getGuild().getMember(user);
          if(member != null) {
            server.getGuild().removeRoleFromMember(member, server.getConfig().getZoeRoleOption().getRole()).queue();
          }
        }
        event.reply(player.getDiscordUser().getName() + " has been deleted !");
      }
    }
  }

  public static Server checkServer(Guild guild) {
    Server server = ServerData.getServers().get(guild.getId());

    if(server == null) {
      server = new Server(guild, SpellingLangage.EN, new ServerConfiguration());
    }
    return server;
  }
}
