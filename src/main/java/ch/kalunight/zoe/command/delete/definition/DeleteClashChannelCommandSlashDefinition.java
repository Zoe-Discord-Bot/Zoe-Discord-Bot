package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.delete.DeleteClashChannelCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class DeleteClashChannelCommandSlashDefinition extends ZoeSlashCommand {

  private EventWaiter eventWaiter;

  public DeleteClashChannelCommandSlashDefinition(EventWaiter waiter, String serverId) {
    this.name = "clashchannel";
    String[] aliases = {"clash", "cc"};
    this.aliases = aliases;
    this.eventWaiter = waiter;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "deleteClashChannelCommandHelpMessageSlashCommand");
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Permission[] botPermissionRequiered = {Permission.MANAGE_CHANNEL, Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION};
    this.botPermissions = botPermissionRequiered;
    
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
    
    DeleteClashChannelCommandRunnable.executeCommand(server, eventWaiter, event.getMember(), null, event.getTextChannel(), event.getHook());
  }

  
  
  
}
