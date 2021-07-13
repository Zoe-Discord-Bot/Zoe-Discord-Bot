package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.CreatePlayerCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;

public class CreatePlayerCommandClassicDefinition extends ZoeCommand {

  public static final String USAGE_NAME = "player";

  public CreatePlayerCommandClassicDefinition() {
    this.name = USAGE_NAME;
    this.help = "createPlayerHelpMessage";
    this.arguments = "@DiscordMentionOfPlayer (Region) (SummonerName)";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommandClassicDefinition.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String message = CreatePlayerCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()), event.getJDA(), event.getMember(),
        event.getMessage().getMentionedMembers(), event.getArgs());
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
