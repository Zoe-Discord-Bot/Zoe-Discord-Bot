package ch.kalunight.zoe.command.stats.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.stats.StatsCommandRunnable;
import ch.kalunight.zoe.command.stats.TeamAnalysisCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class TeamAnalysisCommandClassicDefinition extends ZoeCommand {

  private EventWaiter waiter;
  
  public TeamAnalysisCommandClassicDefinition(EventWaiter eventWaiter) {
    this.name = "teamanalysis";
    String[] aliases = {"team", "analysis", "t"};
    this.aliases = aliases;
    this.arguments = "";
    this.help = "statsTeamAnalysisHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(StatsCommandRunnable.USAGE_NAME, name, arguments, help);
    this.waiter = eventWaiter;
    Permission[] botPermissionNeeded = {Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_ADD_REACTION};
    this.botPermissions = botPermissionNeeded;
    this.guildOnly = true;
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    TeamAnalysisCommandRunnable.executeCommand(server, waiter, event.getTextChannel(), event.getMember());
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}