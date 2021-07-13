package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.create.RegisterCommandRunnable;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class RegisterCommandSlashDefinition extends ZoeSlashCommand {

  public RegisterCommandSlashDefinition(String serverId) {
    this.name = RegisterCommandRunnable.USAGE_NAME;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "registerCommandHelpMessageSlashCommand");
    
    List<OptionData> data = new ArrayList<>();
    
    data.add(CommandUtil.getRegionSelection(true));
    data.add(CommandUtil.getSummonerSelection(true));
    
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
    String args = String.format("(%s) (%s)", event.getOption(ZoeSlashCommand.REGION_OPTION_ID).getAsString(), event.getOption(ZoeSlashCommand.SUMMONER_OPTION_ID).getAsString());
    
    String message = RegisterCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()), event.getGuild(), event.getMember(), args);
    
    event.getHook().editOriginal(message).queue();
  }
  
}
