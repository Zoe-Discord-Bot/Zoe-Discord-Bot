package ch.kalunight.zoe.command.stats;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.team.TeamSelectorAnalysisDataManager;
import ch.kalunight.zoe.model.team.TeamSelectorDataHandler;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class TeamAnalysisCommand extends ZoeCommand {

  private final EventWaiter waiter;
  
  public TeamAnalysisCommand(EventWaiter eventWaiter) {
    this.name = "teamAnalysis";
    String[] aliases = {"team", "analysis", "t"};
    this.aliases = aliases;
    this.arguments = "";
    this.help = "statsTeamAnalysisHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(StatsCommand.USAGE_NAME, name, arguments, help);
    this.waiter = eventWaiter;
    Permission[] botPermissionNeeded = {Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_ADD_REACTION};
    this.botPermissions = botPermissionNeeded;
    this.guildOnly = true;
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    TeamSelectorDataHandler teamDataHandler = new TeamSelectorDataHandler(waiter, server, event, new TeamSelectorAnalysisDataManager(event, server), ServerThreadsManager.getClashChannelExecutor());

    teamDataHandler.askSelectionAccount();
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    // TODO Auto-generated method stub
    return null;
  }

}
