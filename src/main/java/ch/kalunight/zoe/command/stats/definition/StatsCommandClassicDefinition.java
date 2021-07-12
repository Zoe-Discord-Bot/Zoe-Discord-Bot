package ch.kalunight.zoe.command.stats.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.stats.StatsCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.util.CommandUtil;

public class StatsCommandClassicDefinition extends ZoeCommand{

  public StatsCommandClassicDefinition(EventWaiter waiter) {
    this.name = StatsCommandRunnable.USAGE_NAME;
    this.aliases = new String[] {"s"};
    Command[] commandsChildren = {new StatsProfileCommandClassicDefinition(waiter), new PredictRoleCommandClassicDefinition(waiter), new TeamAnalysisCommandClassicDefinition(waiter)};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(StatsCommandRunnable.USAGE_NAME, commandsChildren);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    StatsCommandRunnable.executeCommand(server, event.getMessage().getMentionedMembers(), event.getArgs(), null, event.getTextChannel(), event);
  }
  
  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
