package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.RefreshCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.util.CommandUtil;

public class RefreshCommandClassicDefinition extends ZoeCommand {

  public RefreshCommandClassicDefinition() {
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
  protected void executeCommand(CommandEvent event) throws SQLException {
    String messageToRespond = RefreshCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()));
    event.reply(messageToRespond);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
