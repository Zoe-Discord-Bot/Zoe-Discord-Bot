package ch.kalunight.zoe.command.clash.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.command.SlashCommand;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.clash.ClashCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class ClashCommandSlashDefinition extends ZoeSlashCommand {

  public ClashCommandSlashDefinition(String serverId) {
    this.name = ClashCommandRunnable.USAGE_NAME;
    SlashCommand[] commandsChildren = {new ClashRefreshCommandSlashDefinition(serverId), new ClashAnalyseCommandSlashDefinition(serverId)};
    this.children = commandsChildren;
    
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
    
    ClashCommandRunnable.executeCommand(server, event.getTextChannel());
  }

}
