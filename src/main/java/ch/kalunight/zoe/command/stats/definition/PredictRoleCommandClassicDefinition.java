package ch.kalunight.zoe.command.stats.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.stats.PredictRoleCommandRunnable;
import ch.kalunight.zoe.command.stats.StatsCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.util.CommandUtil;

public class PredictRoleCommandClassicDefinition extends ZoeCommand {

  private EventWaiter waiter;
  
  public PredictRoleCommandClassicDefinition(EventWaiter waiter) {
    this.name = "predictrole";
    String[] aliases = {"role", "predictPosition", "predict"};
    this.aliases = aliases;
    this.arguments = "";
    this.help = "statsPredictRoleHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(StatsCommandRunnable.USAGE_NAME, name, arguments, help);
    this.waiter = waiter;
    this.cooldown = 60;
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    PredictRoleCommandRunnable.executeCommand(server, waiter, event.getTextChannel(), event.getMember());
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
