package ch.kalunight.zoe.command.remove.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.remove.RemovePlayerToTeamCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class RemovePlayerToTeamCommandSlashDefinition extends ZoeSlashCommand {

  public RemovePlayerToTeamCommandSlashDefinition(String serverId) {
    this.name = RemovePlayerToTeamCommandRunnable.USAGE_NAME;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "removePlayerToTeamHelpMessageSlashCommand");
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    
    List<OptionData> data = new ArrayList<>();
    
    OptionData discordAccountToLink = new OptionData(OptionType.USER, ZoeSlashCommand.USER_OPTION_ID, "The discord account you want to delete.");
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
    
    List<Member> mentionnedPlayers = new ArrayList<>();
    mentionnedPlayers.add(event.getOption(ZoeSlashCommand.USER_OPTION_ID).getAsMember());
    
    String message = RemovePlayerToTeamCommandRunnable.executeCommand(server, mentionnedPlayers, event.getJDA());
    
    event.getHook().editOriginal(message).queue();
  }
  
}
