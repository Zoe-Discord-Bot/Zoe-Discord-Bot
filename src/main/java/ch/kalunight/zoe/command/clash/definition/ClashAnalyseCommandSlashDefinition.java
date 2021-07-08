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

  public ClashAnalyseCommandSlashDefinition() {
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
  }
  
  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    Server server = ZoeCommand.getServer(event.getGuild().getIdLong());
    
    event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "loadingData")).queue();
    
    String args = "(" + event.getOptionsByName(ZoeSlashCommand.REGION_OPTION_ID) + ") (" + event.getOptionsByName(ZoeSlashCommand.SUMMONER_OPTION_ID) + ")";
    
    ClashAnalyseCommandRunnable.executeCommand(server, event.getTextChannel(), args, null, event.getHook());
  }

  
  
}
