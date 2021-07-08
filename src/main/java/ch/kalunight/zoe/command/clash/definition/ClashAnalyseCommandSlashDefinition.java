package ch.kalunight.zoe.command.clash.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.clash.ClashAnalyseCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class ClashAnalyseCommandSlashDefinition extends ZoeSlashCommand {

  public ClashAnalyseCommandSlashDefinition(String serverId) {
    this.name = ClashAnalyseCommandRunnable.USAGE_NAME;
    String[] aliases = {"stats"};
    this.arguments = "(Platform) (Summoner Name)";
    this.aliases = aliases;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "clashAnalyzeHelpMessageSlashCommand");
    this.cooldown = 30;
    
    List<OptionData> options = new ArrayList<>();
    
    options.add(CommandUtil.getRegionSelection(true));
    options.add(CommandUtil.getSummonerSelection(true));
    
    this.options = options;
    
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
    
    event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "loadingData")).queue();
    
    String args = "(" + event.getOption(ZoeSlashCommand.REGION_OPTION_ID).getAsString() + ") (" + event.getOption(ZoeSlashCommand.SUMMONER_OPTION_ID).getAsString() + ")";
    
    ClashAnalyseCommandRunnable.executeCommand(server, event.getTextChannel(), args, null, event.getHook());
  }

  
  
}
