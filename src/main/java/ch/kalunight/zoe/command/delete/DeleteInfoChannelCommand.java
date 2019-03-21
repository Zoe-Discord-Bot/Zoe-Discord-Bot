package ch.kalunight.zoe.command.delete;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;

public class DeleteInfoChannelCommand extends Command {
  
  public DeleteInfoChannelCommand() {
    this.name = "infoChannel";
    this.arguments = "";
    this.help = "Delete the infoChannel after the refresh. Manage Channel permission needed.";
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
    
    if(server.getInfoChannel() == null) {
      event.reply("The info channel is not defined!");
    }else {
      try {
        server.getInfoChannel().delete().queue();
      } catch (InsufficientPermissionException e) {
        server.setInfoChannel(null);
        server.setControlePannel(new ControlPannel());
        event.reply("The info channel has been deleted **INSIDE THE SYSTEME**! I don't have the permission to delete the text channel!");
        return;
      }

      server.setInfoChannel(null);
      server.setControlePannel(new ControlPannel());
      event.reply("The info channel has been deleted !");
    }
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Delete infoChannel command :\n");
        stringBuilder.append("--> `>delete " + name + " " + arguments + "` : " + help);
        
        event.reply(stringBuilder.toString());
      }
    };
  }
}
