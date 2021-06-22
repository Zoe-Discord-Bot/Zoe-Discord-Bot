package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;

import ch.kalunight.zoe.command.RefreshCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class RefreshCommandSlashDefinition extends ZoeSlashCommand {

  public RefreshCommandSlashDefinition(String serverId) {
    this.name = "refresh";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "refreshCommandHelp");
    this.hidden = false;
    this.ownerCommand = false;
    this.cooldown = 120;
    
    if(serverId == null) {
      this.guildOnly = true;
      this.guildId = null;
    }else {
      this.guildOnly = true; //True for testing
      this.guildId = serverId; //Test server
    }
  }
  
  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    String message = RefreshCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()));
    event.getHook().editOriginal(message).queue();
  }
  
}
