package ch.kalunight.zoe.command.show.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.show.ShowPlayerCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class ShowPlayerCommandSlashDefinition extends ZoeSlashCommand {

  private final EventWaiter waiter;

  public ShowPlayerCommandSlashDefinition(EventWaiter eventWaiter, String serverId) {
    this.name = ShowPlayerCommandRunnable.USAGE_NAME;
    this.waiter = eventWaiter;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "showPlayerHelpMessageSlashCommand");
    this.cooldown = 10;
    Permission[] botPermissionNeeded = {Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_MANAGE};
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
    
    event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "loadingSummoner")).queue();
    
    ShowPlayerCommandRunnable.executeCommand(server, waiter, event.getMember(), event.getTextChannel(), null, event.getHook());
  }
  
}
