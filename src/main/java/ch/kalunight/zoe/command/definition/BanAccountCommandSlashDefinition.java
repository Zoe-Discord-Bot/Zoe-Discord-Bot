package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.BanAccountCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class BanAccountCommandSlashDefinition extends ZoeSlashCommand {

  private EventWaiter waiter;

  public BanAccountCommandSlashDefinition(EventWaiter eventWaiter, String serverId) {
    this.name = "banaccount";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "banAccountCommandHelpSlashMessage");
    this.waiter = eventWaiter;
    this.hidden = false;
    this.ownerCommand = false;
    this.cooldown = 180;
        
    if(serverId == null) {
      this.guildOnly = false;
    }else {
      this.guildOnly = true; //True for testing
      this.guildId = serverId; //Test server
    }
  }

  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    String language = LanguageManager.DEFAULT_LANGUAGE;
    if(event.getGuild() != null) {
      Server server = ZoeCommand.getServer(event.getGuild().getIdLong());
      language = server.getLanguage();
    }
    
    event.getHook().editOriginal(LanguageManager.getText(language, "loading")).queue();
    BanAccountCommandRunnable.executeCommand(language, waiter, event.getChannel(), event.getGuild(), event.getUser(), null, event.getHook());
  }
  
}
