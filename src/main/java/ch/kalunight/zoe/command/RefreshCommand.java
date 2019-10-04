package ch.kalunight.zoe.command;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;

public class RefreshCommand extends Command {
  
  public RefreshCommand() {
    this.name = "refresh";
    String[] aliases = {"r"};
    this.aliases = aliases;
    this.help = "refreshCommandHelp";
    this.hidden = false;
    this.ownerCommand = false;
    this.guildOnly = true;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
    this.cooldown = 120;
  }

  @Override
  protected void execute(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    ServerData.getServersAskedTreatment().put(server.getGuild().getId(), true);
    event.reply("The information panel will be refreshed in a few seconds.");
  }
}
