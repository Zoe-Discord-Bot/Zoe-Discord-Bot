package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.create.CreateTeamCommandRunnable;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CreateTeamCommandSlashDefinition extends ZoeSlashCommand {

  public CreateTeamCommandSlashDefinition(String serverId) {
    this.name = CreateTeamCommandRunnable.USAGE_NAME;
    this.arguments = "nameOfTheTeam";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "createTeamHelpMessageSlashCommand");
    
    List<OptionData> data = new ArrayList<>();
    OptionData teamName = new OptionData(OptionType.STRING, "team-name", "Name of the team");
    teamName.setRequired(true);
    
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
    String message = CreateTeamCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()), event.getOption("team-name").getAsString());
    event.getHook().editOriginal(message).queue();
  }
  
}
