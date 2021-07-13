package ch.kalunight.zoe.command.remove.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.remove.RemoveCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;

public class RemoveCommandClassicDefinition extends ZoeCommand {

  public RemoveCommandClassicDefinition() {
    this.name = RemoveCommandRunnable.USAGE_NAME;
    Command[] commandsChildren = {new RemovePlayerToTeamCommandClassicDefinition(), new RemoveAccountCommandClassicDefinition()};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(RemoveCommandRunnable.USAGE_NAME, commandsChildren);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String message = RemoveCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()));
    
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}