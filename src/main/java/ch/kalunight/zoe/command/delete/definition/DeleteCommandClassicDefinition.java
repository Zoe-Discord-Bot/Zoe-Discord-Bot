package ch.kalunight.zoe.command.delete.definition;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.delete.DeleteClashChannelCommand;
import ch.kalunight.zoe.command.delete.DeleteCommandRunnable;
import ch.kalunight.zoe.command.delete.DeleteInfoChannelCommand;
import ch.kalunight.zoe.command.delete.DeleteLeaderboardCommand;
import ch.kalunight.zoe.command.delete.DeleteRankHistoryChannelCommand;
import ch.kalunight.zoe.command.delete.DeleteTeamCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class DeleteCommandClassicDefinition extends ZoeCommand {

  public static final String USAGE_NAME = "delete";

  public DeleteCommandClassicDefinition(EventWaiter waiter) {
    this.name = USAGE_NAME;
    this.aliases = new String[] {"d"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Command[] commandsChildren = {new DeletePlayerCommandClassicDefinition(), new DeleteInfoChannelCommand(), new DeleteTeamCommand(),
        new DeleteRankHistoryChannelCommand(), new DeleteLeaderboardCommand(waiter), new DeleteClashChannelCommand(waiter)};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    String message = DeleteCommandRunnable.executeCommand(server);
    
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
