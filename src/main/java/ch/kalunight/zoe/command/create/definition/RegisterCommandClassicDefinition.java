package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.RegisterCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;

public class RegisterCommandClassicDefinition extends ZoeCommand {

  private EventWaiter waiter;
  
  public RegisterCommandClassicDefinition(EventWaiter waiter) {
    this.name = RegisterCommandRunnable.USAGE_NAME;
    this.arguments = "(Region) (SummonerName)";
    this.help = "registerCommandHelpMessage";
    this.waiter = waiter;
    this.helpBiConsumer = CommandUtil.getHelpMethod(RegisterCommandRunnable.USAGE_NAME, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String message = RegisterCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()), event.getGuild(), event.getMember(),
        event.getArgs(), waiter, event.getTextChannel());
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
