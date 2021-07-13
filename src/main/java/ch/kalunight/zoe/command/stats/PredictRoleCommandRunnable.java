package ch.kalunight.zoe.command.stats;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.team.TeamSelectorDataHandler;
import ch.kalunight.zoe.model.team.TeamSelectorPredictRoleDataManager;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class PredictRoleCommandRunnable {

  private PredictRoleCommandRunnable() {
    // hide default constructor
  }
  
  public static void executeCommand(Server server, EventWaiter waiter, TextChannel channel, Member author) {
    
    TeamSelectorDataHandler teamDataHandler = new TeamSelectorDataHandler(waiter, server, channel, author, new TeamSelectorPredictRoleDataManager(server, channel), ServerThreadsManager.getClashChannelExecutor());

    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "statsPredictRoleAnalysisStart")).queue();
    
    teamDataHandler.askSelectionAccount();
  }

}
