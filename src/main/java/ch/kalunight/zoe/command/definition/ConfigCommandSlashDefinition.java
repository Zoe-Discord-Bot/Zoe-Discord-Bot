package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ConfigCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class ConfigCommandSlashDefinition extends ZoeSlashCommand {

  private EventWaiter waiter;
  
  public ConfigCommandSlashDefinition(EventWaiter waiter, String serverId) {
    this.name = "config";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "configCommandHelpSlashCommand");
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL, Permission.MESSAGE_ADD_REACTION};
    this.userPermissions = permissionRequired;
    Permission[] permissionBot = {Permission.MESSAGE_ADD_REACTION, Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS};
    this.botPermissions = permissionBot;
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
    
    event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "loading")).queue();
    
    CommandGuildDiscordData data = new CommandGuildDiscordData(event.getMember(), event.getGuild(), event.getTextChannel());
    
    ConfigCommandRunnable.executeCommand(server, waiter, data, event.getHook());
  }
  
}
