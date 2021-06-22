package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.create.CreateInfoChannelCommandRunnable;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CreateInfochannelCommandSlashDefinition extends ZoeSlashCommand {

  public CreateInfochannelCommandSlashDefinition(String serverId) {
    this.name = "infochannel";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "createInfoChannelHelpMessage");
    
    List<OptionData> data = new ArrayList<>();
    
    OptionData infochannelName = new OptionData(OptionType.STRING, "infochannel-name", "The name for the infochannel");
    infochannelName.setRequired(true);
    
    data.add(infochannelName);
    
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
    String message = CreateInfoChannelCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()), event.getOption("infochannel-name").getAsString(), event.getGuild());
    event.getHook().editOriginal(message).queue();
  }
  
}
