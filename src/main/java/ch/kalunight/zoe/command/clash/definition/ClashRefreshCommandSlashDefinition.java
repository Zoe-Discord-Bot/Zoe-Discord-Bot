package ch.kalunight.zoe.command.clash.definition;

import java.sql.SQLException;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.clash.ClashRefreshCommandRunnable;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class ClashRefreshCommandSlashDefinition extends ZoeSlashCommand {

  public ClashRefreshCommandSlashDefinition(String serverId) {
    this.name = ClashRefreshCommandRunnable.USAGE_NAME;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "clashRefreshHelpMessageSlashCommand");
    this.cooldown = 10;
    
    if(serverId == null) {
      this.guildOnly = true;
    }else {
      this.guildOnly = true; //True for testing
      this.guildId = serverId; //Test server
    }
  }

  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    ClashRefreshCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()), event.getTextChannel(), event.getHook());
  }
}
