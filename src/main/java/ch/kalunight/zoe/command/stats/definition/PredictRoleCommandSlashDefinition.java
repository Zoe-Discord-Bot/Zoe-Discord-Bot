package ch.kalunight.zoe.command.stats.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.stats.PredictRoleCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class PredictRoleCommandSlashDefinition extends ZoeSlashCommand {

  private EventWaiter waiter;
  
  public PredictRoleCommandSlashDefinition(EventWaiter waiter, String serverId) {
    this.name = "predictrole";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "statsPredictRoleHelpMessageSlashMessage");
    this.waiter = waiter;
    this.cooldown = 60;
    
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
    
    PredictRoleCommandRunnable.executeCommand(server, waiter, event.getTextChannel(), event.getMember(), false);
  }
  
}
