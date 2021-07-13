package ch.kalunight.zoe.command.show.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.show.ShowCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class ShowCommandClassicDefinition extends ZoeCommand {
  
  public ShowCommandClassicDefinition(EventWaiter waiter) {
    this.name = ShowCommandRunnable.USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Command[] commandsChildren = {new ShowPlayerCommandClassicDefinition(waiter)};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(ShowCommandRunnable.USAGE_NAME, commandsChildren);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String message = ShowCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()));
    
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
