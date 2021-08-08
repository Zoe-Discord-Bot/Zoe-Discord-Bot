package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import ch.kalunight.zoe.command.WikiCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class WikiCommandSlashDefinition extends ZoeSlashCommand {

  public WikiCommandSlashDefinition(String serverId) {
    this.name = "wiki";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "wikiCommandSlashHelp");
    this.hidden = false;
    this.ownerCommand = false;
    
    if(serverId == null) {
      this.guildOnly = false;
    }else {
      this.guildOnly = true; //True for testing
      this.guildId = serverId; //Test server
    }
  }

  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    String language;
    if(event.getGuild() != null) {
      language = ZoeCommand.getServer(event.getGuild().getIdLong()).getLanguage();
    }else {
      language = LanguageManager.DEFAULT_LANGUAGE;
    }
    
    event.getHook().editOriginal(WikiCommandRunnable.executeCommand(language)).queue();
  }
  
}
