package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.delete.DeletePlayerRunnable;
import ch.kalunight.zoe.util.CommandUtil;

public class DeletePlayerCommandClassicDefinition extends ZoeCommand {

  public static final String USAGE_NAME = "delete";
  
  public DeletePlayerCommandClassicDefinition() {
    this.name = DeletePlayerRunnable.USAGE_NAME;
    this.help = "deletePlayerHelpMessage";
    this.arguments = "@DiscordMentionOfPlayer";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(USAGE_NAME, DeletePlayerRunnable.USAGE_NAME, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String message = DeletePlayerRunnable.executeCommand(getServer(event.getGuild().getIdLong()), event.getMember(), event.getMessage().getMentionedMembers());
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
