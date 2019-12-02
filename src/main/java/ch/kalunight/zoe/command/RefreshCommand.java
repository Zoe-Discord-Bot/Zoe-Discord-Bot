package ch.kalunight.zoe.command;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;

public class RefreshCommand extends ZoeCommand {
  
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
  protected void executeCommand(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    ServerData.getServersAskedTreatment().put(server.getGuild().getId(), true);
    event.reply(LanguageManager.getText(server.getLangage(), "refreshCommandDoneMessage"));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
