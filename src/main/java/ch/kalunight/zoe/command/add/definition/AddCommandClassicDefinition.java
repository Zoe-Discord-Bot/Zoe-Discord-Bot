package ch.kalunight.zoe.command.add.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.add.AddCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;

public class AddCommandClassicDefinition extends ZoeCommand{

  public AddCommandClassicDefinition() {
    this.name = AddCommandRunnable.USAGE_NAME;
    this.arguments = "";
    Command[] commandsChildren = {new AddPlayerToTeamCommandClassicDefinition(), new AddAccountCommandClassicDefinition()};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(AddCommandRunnable.USAGE_NAME, commandsChildren);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String message = AddCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()));
    
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
