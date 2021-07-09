package ch.kalunight.zoe.command.add.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.add.AddPlayerToTeamCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class AddPlayerToTeamCommandSlashDefinition extends ZoeSlashCommand {

  public AddPlayerToTeamCommandSlashDefinition(String serverId) {
    this.name = AddPlayerToTeamCommandRunnable.USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.arguments = "@MentionPlayer (teamName)";
    this.userPermissions = permissionRequired;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "addPlayerToTeamCommandHelpSlashCommand");
    
    List<OptionData> data = new ArrayList<>();
    
    OptionData discordAccountToLink = new OptionData(OptionType.USER, ZoeSlashCommand.USER_OPTION_ID, "The discord account you want to link.");
    discordAccountToLink.setRequired(true);
    
    OptionData teamName = new OptionData(OptionType.STRING, "team-name", "Name of the team");
    teamName.setRequired(true);
    
    data.add(discordAccountToLink);
    data.add(teamName);
    
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
    
    List<Member> mentionnedMembers = new ArrayList<>();
    mentionnedMembers.add(event.getOption(ZoeSlashCommand.USER_OPTION_ID).getAsMember());
    
    String args = "(" + event.getOption("team-name").getAsString() + ")";
    
    String message = AddPlayerToTeamCommandRunnable.executeCommand(server, mentionnedMembers, args);
    
    event.getHook().editOriginal(message).queue();
  }

}
