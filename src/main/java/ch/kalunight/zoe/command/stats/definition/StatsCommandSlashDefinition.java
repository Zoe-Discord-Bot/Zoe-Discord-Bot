package ch.kalunight.zoe.command.stats.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.stats.StatsCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class StatsCommandSlashDefinition extends ZoeSlashCommand {

  public StatsCommandSlashDefinition(EventWaiter waiter, String serverId) {
    this.name = StatsCommandRunnable.USAGE_NAME;
    this.aliases = new String[] {"s"};
    SlashCommand[] commandsChildren = {new StatsProfileCommandSlashDefinition(waiter, serverId), new PredictRoleCommandSlashDefinition(waiter, serverId),
        new TeamAnalysisCommandSlashDefinition(waiter, serverId)};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(StatsCommandRunnable.USAGE_NAME, commandsChildren);
  }
  
  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    Server server = ZoeCommand.getServer(event.getGuild().getIdLong());
    
    List<Member> mentionnedUser = new ArrayList<>();
    
    StatsCommandRunnable.executeCommand(server, mentionnedUser, "", event.getHook(), event.getTextChannel(), null);
  }
}
