package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.delete.DeleteLeaderboardCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class DeleteLeaderboardCommandSlashDefinition extends ZoeSlashCommand {

  private EventWaiter eventWaiter;

  public DeleteLeaderboardCommandSlashDefinition(EventWaiter waiter, String serverId) {
    this.name = "leaderboard";
    this.eventWaiter = waiter;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "deleteLeaderboardCommandHelpMessageSlashMessage");
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    
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
    
    event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "loadingData")).queue();
    
    DeleteLeaderboardCommandRunnable.executeCommand(server, event.getMember(), event.getTextChannel(), null, eventWaiter, event.getHook());
  }

  
  
}
