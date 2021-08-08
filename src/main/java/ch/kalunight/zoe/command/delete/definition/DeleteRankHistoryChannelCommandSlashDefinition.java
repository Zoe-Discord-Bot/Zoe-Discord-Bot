package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.delete.DeleteRankHistoryChannelCommandRunnable;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class DeleteRankHistoryChannelCommandSlashDefinition extends ZoeSlashCommand {

  public DeleteRankHistoryChannelCommandSlashDefinition(String serverId) {
    this.name = "rankchannel";
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "deleteRankChannelHelpMessageSlashCommand");
    
    if(serverId == null) {
      this.guildOnly = true;
    }else {
      this.guildOnly = true; //True for testing
      this.guildId = serverId; //Test server
    }
  }

  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    event.getHook().editOriginal(DeleteRankHistoryChannelCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()), event.getGuild())).queue();
  }
  
}
