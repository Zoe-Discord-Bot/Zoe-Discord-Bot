package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.CreatePlayerCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;

public class CreatePlayerCommandClassicDefinition extends ZoeCommand {

  public static final String USAGE_NAME = "player";

  private EventWaiter waiter;
  
  public CreatePlayerCommandClassicDefinition(EventWaiter waiter) {
    this.name = USAGE_NAME;
    this.help = "createPlayerHelpMessage";
    this.arguments = "@DiscordMentionOfPlayer (Region) (SummonerName)";
    this.waiter = waiter;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommandClassicDefinition.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String message = CreatePlayerCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()), event.getJDA(), event.getMember(),
        event.getMessage().getMentionedMembers(), event.getArgs(), waiter, event.getTextChannel());
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
