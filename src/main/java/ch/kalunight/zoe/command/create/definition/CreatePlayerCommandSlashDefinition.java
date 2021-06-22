package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.create.CreatePlayerCommandRunnable;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CreatePlayerCommandSlashDefinition extends ZoeSlashCommand {

  public CreatePlayerCommandSlashDefinition(String serverId) {
    this.name = CreatePlayerCommandClassicDefinition.USAGE_NAME;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "createPlayerHelpMessage");
    
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    
    List<OptionData> data = new ArrayList<>();
    
    OptionData discordAccountToLink = new OptionData(OptionType.USER, ZoeSlashCommand.USER_OPTION_ID, "The discord account you want to link");
    discordAccountToLink.setRequired(true);
    
    OptionData summoner = new OptionData(OptionType.STRING, ZoeSlashCommand.SUMMONER_OPTION_ID, "The summoner name of the wanted league account");
    summoner.setRequired(true);
    
    data.add(discordAccountToLink);
    data.add(CommandUtil.getRegionSelection(true));
    data.add(summoner);
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
    
    List<Member> mentionnedMembers = new ArrayList<>();
    mentionnedMembers.add(event.getOption("user").getAsMember());
    
    String args = String.format("(%s) (%s)", event.getOption(ZoeSlashCommand.REGION_OPTION_ID).getAsString(), event.getOption(ZoeSlashCommand.SUMMONER_OPTION_ID).getAsString());
    
    String message = CreatePlayerCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()), event.getJDA(), event.getMember(), mentionnedMembers, args);
    
    event.getHook().editOriginal(message).queue();
  }
  
}
