package ch.kalunight.zoe.command.delete;

import java.util.List;
import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

public class DeletePlayerCommand extends Command {

  public DeletePlayerCommand() {
    this.name = "player";
    this.help = "Delete the given player. Manage Channel permission needed.";
    this.arguments = "@DiscordMentionOfPlayer";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    Server server = ServerData.getServers().get(event.getGuild().getId());

    if(server == null) {
      server = new Server(event.getGuild(), SpellingLangage.EN);
    }

    List<Member> members = event.getMessage().getMentionedMembers();

    if(members.size() != 1) {
      event.reply("You need to mention 1 people !");
    } else {
      User user = members.get(0).getUser();
      Player player = server.getPlayerByDiscordId(user.getId());

      if(player == null) {
        event.reply("This people is not registered !");
      } else {
        server.deletePlayer(player);
        event.reply("This people has been deleted !");
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
