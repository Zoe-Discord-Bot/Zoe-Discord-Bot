package ch.kalunight.zoe.command;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerThreadsManager;

public class RebootCommand extends ZoeCommand {

  public RebootCommand() {
    this.name = "reboot";
    this.help = "Safely reboot the bot";
    this.hidden = true;
    this.ownerCommand = true;
    this.guildOnly = false;
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    ServerThreadsManager.setRebootAsked(true);
    event.reply("Reboot asked, will be done in next seconds ...");
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return null;
  }

}
