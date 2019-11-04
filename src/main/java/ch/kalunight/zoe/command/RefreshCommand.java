package ch.kalunight.zoe.command;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.util.CommandUtil;

public class RefreshCommand extends ZoeCommand {
  
  public RefreshCommand() {
    this.name = "refresh";
    String[] aliases = {"r"};
    this.aliases = aliases;
    this.help = "Refresh manually the info pannel of the server.";
    this.hidden = false;
    this.ownerCommand = false;
    this.guildOnly = true;
    this.helpBiConsumer = getHelpMethod();
    this.cooldown = 120;
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    ServerData.getServersAskedTreatment().put(server.getGuild().getId(), true);
    event.reply("The information panel will be refreshed in a few seconds.");
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Refresh command :\n");
        stringBuilder.append("--> `>" + name + " " + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
  
}
