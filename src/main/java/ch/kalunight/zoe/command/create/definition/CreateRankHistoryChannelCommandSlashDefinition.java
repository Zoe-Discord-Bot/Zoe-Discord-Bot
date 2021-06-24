package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.create.CreateRankHistoryChannelCommandRunnable;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CreateRankHistoryChannelCommandSlashDefinition extends ZoeSlashCommand {

  public CreateRankHistoryChannelCommandSlashDefinition(String serverId) {
    this.name = "rankchannel";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "createRankChannelHelpMessageSlashCommand");
    
    List<OptionData> data = new ArrayList<>();
    
    OptionData stringRankChannel = new OptionData(OptionType.STRING, "rankchannel-name", "The name of the rank channel");
    stringRankChannel.setRequired(true);
    
    data.add(stringRankChannel);
    
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
    String message = CreateRankHistoryChannelCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()), event.getOption("rankchannel-name").getAsString(), event.getGuild());
    event.getHook().editOriginal(message).queue();
  }

}
