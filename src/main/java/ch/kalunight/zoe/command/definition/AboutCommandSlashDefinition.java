package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;

import ch.kalunight.zoe.command.AboutCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class AboutCommandSlashDefinition extends ZoeSlashCommand {

  public AboutCommandSlashDefinition(String serverId) {
    this.name = "about";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "aboutCommandHelp");
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
    MessageEmbed embed = AboutCommandRunnable.executeCommand(event.getJDA(), ZoeCommand.getLanguage(event.getGuild()));
    event.getHook().editOriginalEmbeds(embed).queue();
  }

}
