package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.CreateCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class CreateCommandClassicDefinition extends ZoeCommand {

  public static final String USAGE_NAME = "create";
  
  public CreateCommandClassicDefinition(EventWaiter waiter) {
    this.name = USAGE_NAME;
    this.aliases = new String[] {"c"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Command[] commandsChildren = {new CreateInfochannelCommandClassicDefinition(), new CreatePlayerCommandClassicDefinition(), new CreateTeamCommandClassicDefinition(),
        new CreateRankHistoryChannelCommandClassicDefinition(), new CreateLeaderboardCommandClassicDefinition(waiter), new CreateClashChannelCommandClassicDefinition(waiter)};
    this.children = commandsChildren;
    this.guildOnly = true;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String message = CreateCommandRunnable.executeCommand(getLanguage(event.getGuild()));
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
