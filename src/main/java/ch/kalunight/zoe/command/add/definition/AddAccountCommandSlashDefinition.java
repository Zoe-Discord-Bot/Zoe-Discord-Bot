package ch.kalunight.zoe.command.add.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.add.AddAccountCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class AddAccountCommandSlashDefinition extends ZoeSlashCommand {

  public AddAccountCommandSlashDefinition(String serverId) {
    this.name = AddAccountCommandRunnable.USAGE_NAME;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "addAccountHelpMessageSlashCommand");
    
    List<OptionData> data = new ArrayList<>();
    
    OptionData discordAccountToLink = new OptionData(OptionType.USER, ZoeSlashCommand.USER_OPTION_ID, "The discord account you want to link.");
    discordAccountToLink.setRequired(true);
    
    data.add(discordAccountToLink);
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
    Server server = ZoeCommand.getServer(event.getGuild().getIdLong());
    
    event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "loadingData")).queue();
    
    List<Member> mentionnedMembers = new ArrayList<>();
    mentionnedMembers.add(event.getOption(ZoeSlashCommand.USER_OPTION_ID).getAsMember());
    
    String args = "(" + event.getOption(ZoeSlashCommand.REGION_OPTION_ID).getAsString() + ") (" + event.getOption(ZoeSlashCommand.SUMMONER_OPTION_ID).getAsString() + ")";
    
    String messageToSend = AddAccountCommandRunnable.executeCommand(server, event.getMember(), mentionnedMembers, args);
    
    event.getHook().editOriginal(messageToSend).queue();
  }

}
