package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.delete.DeleteInfoChannelCommandRunnable;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class DeleteInfoChannelCommandSlashDefinition extends ZoeSlashCommand {

  public DeleteInfoChannelCommandSlashDefinition(String serverId) {
    this.name = "infochannel";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "deleteInfoChannelHelpMessageSlashCommand");
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
    String message = DeleteInfoChannelCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()), event.getGuild());
    event.getHook().editOriginal(message).queue();
  }
  
}
