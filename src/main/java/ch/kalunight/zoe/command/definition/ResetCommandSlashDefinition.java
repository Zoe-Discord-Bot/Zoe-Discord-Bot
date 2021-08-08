package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ResetCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class ResetCommandSlashDefinition extends ZoeSlashCommand {

  private final EventWaiter waiter;

  public ResetCommandSlashDefinition(EventWaiter waiter, String serverId) {
    this.name = "reset";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "resetCommandHelpSlashCommand");
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
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
    ResetCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()), event.getTextChannel(), event.getHook(), waiter, event.getUser());
  }
  
}
