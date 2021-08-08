package ch.kalunight.zoe.command.stats.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.stats.TeamAnalysisCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class TeamAnalysisCommandSlashDefinition extends ZoeSlashCommand {

  private EventWaiter waiter;
  
  public TeamAnalysisCommandSlashDefinition(EventWaiter eventWaiter, String serverId) {
    this.name = "teamanalysis";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "statsTeamAnalysisHelpMessageSlashCommand");
    this.waiter = eventWaiter;
    Permission[] botPermissionNeeded = {Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_ADD_REACTION};
    this.botPermissions = botPermissionNeeded;
    
    if(serverId == null) {
      this.guildOnly = true;
    }else {
      this.guildOnly = true; //True for testing
      this.guildId = serverId; //Test server
    }
  }

  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    Server server = ZoeCommand.getServer(event.getGuild().getIdLong());
    
    event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "loading")).queue();
    
    TeamAnalysisCommandRunnable.executeCommand(server, waiter, event.getTextChannel(), event.getMember());
  }
  
}
