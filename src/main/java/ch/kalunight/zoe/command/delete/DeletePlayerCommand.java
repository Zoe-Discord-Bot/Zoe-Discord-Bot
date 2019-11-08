package ch.kalunight.zoe.command.delete;

import java.util.List;
import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class DeletePlayerCommand extends ZoeCommand {

  public static final String USAGE_NAME = "player";

  public DeletePlayerCommand() {
    this.name = USAGE_NAME;
    this.help = "Delete the given player.";
    this.arguments = "@DiscordMentionOfPlayer";
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(!server.getConfig().getUserSelfAdding().isOptionActivated() && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
        event.reply("You need the permission \"" + Permission.MANAGE_CHANNEL.getName() + "\" to do that.");
        return;
    }
    
    List<Member> members = event.getMessage().getMentionedMembers();

    if(members.size() != 1) {
      event.reply("You need to mention 1 people !");
    } else {
      User user = members.get(0).getUser();
      
      if(!user.equals(event.getAuthor()) && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
        event.reply("You cannot delete another player than you if you don't have the *" + Permission.MANAGE_CHANNEL.getName() + "* permission.");
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

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Delete player command :\n");
        stringBuilder.append("--> `>delete " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
}
