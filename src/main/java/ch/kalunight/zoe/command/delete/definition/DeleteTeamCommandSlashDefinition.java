package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.delete.DeleteTeamCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class DeleteTeamCommandSlashDefinition extends ZoeSlashCommand {

  public DeleteTeamCommandSlashDefinition(String serverId) {
    this.name = DeleteTeamCommandRunnable.USAGE_NAME;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "deleteTeamHelpMessageSlashCommand");
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    
    List<OptionData> data = new ArrayList<>();
    
    OptionData discordAccountToLink = new OptionData(OptionType.STRING, "team-name", "The team you want to delete.");
    discordAccountToLink.setRequired(true);
    
    data.add(discordAccountToLink);
    
    this.options = data;
    
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
    
    String teamName = event.getOption("team-name").getAsString();
    
    String message = DeleteTeamCommandRunnable.executeCommand(server, teamName);
    event.getHook().editOriginal(message).queue();
  }
  
}
