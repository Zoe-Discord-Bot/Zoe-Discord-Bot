package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;

import ch.kalunight.zoe.command.PatchNotesCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class PatchNotesCommandSlashDefinition extends ZoeSlashCommand {

  public PatchNotesCommandSlashDefinition(String serverId) {
    this.name = "patchnotes";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "patchNotesCommandSlashCommand");
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
    event.getHook().editOriginal(PatchNotesCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()))).queue();
  }

}
