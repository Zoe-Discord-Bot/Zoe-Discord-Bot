package ch.kalunight.zoe.command.stats;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.team.TeamSelectorAnalysisDataManager;
import ch.kalunight.zoe.model.team.TeamSelectorDataHandler;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class TeamAnalysisCommandRunnable {
  
  private TeamAnalysisCommandRunnable() {
    // hide default constructor
  }
  
  public static void executeCommand(Server server, EventWaiter waiter, TextChannel channel, Member member) {
    TeamSelectorDataHandler teamDataHandler = new TeamSelectorDataHandler(waiter, server, channel, member, new TeamSelectorAnalysisDataManager(server, channel, false), ServerThreadsManager.getClashChannelExecutor());

    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisStart")).queue();
    
    teamDataHandler.askSelectionAccount();
  }

}
