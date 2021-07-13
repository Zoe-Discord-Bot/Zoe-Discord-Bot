package ch.kalunight.zoe.command.remove.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.command.SlashCommand;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.remove.RemoveCommandRunnable;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class RemoveCommandSlashDefinition extends ZoeSlashCommand {

  public RemoveCommandSlashDefinition(String serverId) {
    this.name = RemoveCommandRunnable.USAGE_NAME;
    SlashCommand[] commandsChildren = {new RemovePlayerToTeamCommandSlashDefinition(serverId), new RemoveAccountCommandSlashDefinition(serverId)};
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
    String message = RemoveCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()));
    
    event.getHook().editOriginal(message).queue();
  }

}
