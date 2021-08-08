package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ResetCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class ResetCommandClassicDefinition extends ZoeCommand {

  private final EventWaiter waiter;

  public ResetCommandClassicDefinition(EventWaiter waiter) {
    this.name = "reset";
    this.help = "resetCommandHelp";
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.waiter = waiter;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    ResetCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()), event.getTextChannel(), null, waiter, event.getAuthor());
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
