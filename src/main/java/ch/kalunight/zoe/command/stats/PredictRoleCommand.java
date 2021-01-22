package ch.kalunight.zoe.command.stats;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.team.TeamSelectorDataHandler;
import ch.kalunight.zoe.model.team.TeamSelectorPredictRoleDataManager;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;

public class PredictRoleCommand extends ZoeCommand {

  private EventWaiter waiter;

  public PredictRoleCommand(EventWaiter waiter) {
    this.name = "predictRole";
    String[] aliases = {"role", "predictPosition", "predict"};
    this.aliases = aliases;
    this.arguments = "";
    this.help = "statsPredictRoleHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(StatsCommand.USAGE_NAME, name, arguments, help);
    this.waiter = waiter;
    this.cooldown = 60;
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    TeamSelectorDataHandler teamDataHandler = new TeamSelectorDataHandler(waiter, server, event, new TeamSelectorPredictRoleDataManager(event, server), ServerThreadsManager.getClashChannelExecutor());

    event.reply(LanguageManager.getText(server.getLanguage(), "statsPredictRoleAnalysisStart"));
    
    teamDataHandler.askSelectionAccount();
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
