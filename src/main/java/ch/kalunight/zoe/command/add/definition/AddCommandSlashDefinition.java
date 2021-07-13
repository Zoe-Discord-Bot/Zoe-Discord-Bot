package ch.kalunight.zoe.command.add.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.command.SlashCommand;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.add.AddCommandRunnable;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class AddCommandSlashDefinition extends ZoeSlashCommand {

  public AddCommandSlashDefinition(String serverId) {
    this.name = AddCommandRunnable.USAGE_NAME;
    SlashCommand[] commandsChildren = {new AddAccountCommandSlashDefinition(serverId), new AddPlayerToTeamCommandSlashDefinition(serverId)};
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
    String message = AddCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()));
    
    event.getHook().editOriginal(message).queue();
  }
  
}
