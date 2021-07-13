package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.create.CreateLeaderboardCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class CreateLeaderboardCommandSlashDefinition extends ZoeSlashCommand {

  private EventWaiter waiter;

  public CreateLeaderboardCommandSlashDefinition(EventWaiter waiter, String serverId) {
    this.name = "leaderboard";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "createLeaderboardHelpMessageSlashCommand");
    this.waiter = waiter;
    
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
    
    CreateLeaderboardCommandRunnable.executeCommand(server, event.getMember(), event.getTextChannel(), null, waiter, event.getHook());
  }

}
