package ch.kalunight.zoe.command.stats.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.stats.StatsCommandRunnable;
import ch.kalunight.zoe.command.stats.StatsProfileCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class StatsProfileCommandSlashDefinition extends ZoeSlashCommand {

  private EventWaiter waiter;
  
  public StatsProfileCommandSlashDefinition(EventWaiter eventWaiter, String serverId) {
    this.name = "profile";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "statsProfileHelpMessageSlashCommand");
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(StatsCommandRunnable.USAGE_NAME, name, arguments, help);
    this.waiter = eventWaiter;
    Permission[] botPermissionNeeded = {Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_MANAGE};
    this.botPermissions = botPermissionNeeded;
    
    List<OptionData> data = new ArrayList<>();
    
    OptionData player = new OptionData(OptionType.USER, ZoeSlashCommand.USER_OPTION_ID, "The player to show.", false);
    
    data.add(player);
    data.add(CommandUtil.getRegionSelection(false));
    data.add(CommandUtil.getSummonerSelection(false));
    
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
    
    OptionMapping playerOption = event.getOption(ZoeSlashCommand.USER_OPTION_ID);
    
    Member selectedMember = null;
    if(playerOption != null) {
      selectedMember = playerOption.getAsMember();
    }
    
    OptionMapping regionOption = event.getOption(ZoeSlashCommand.REGION_OPTION_ID);
    
    String region = null;
    if(regionOption != null) {
      region = regionOption.getAsString();
    }
    
    OptionMapping summonerOption = event.getOption(ZoeSlashCommand.SUMMONER_OPTION_ID);
    
    String summonerName = null;
    if(summonerOption != null) {
      summonerName = summonerOption.getAsString();
    }
    
    if(selectedMember != null || (summonerName != null && region != null)) {
      
      event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "loadingData")).queue();
      
      if(selectedMember != null) {
        List<User> mentionnedUser = new ArrayList<>();
        mentionnedUser.add(selectedMember.getUser());
        StatsProfileCommandRunnable.executeCommand(server, event.getTextChannel(), "", mentionnedUser, null, event.getHook(), waiter, event.getMember(), false);
        return;
      }
      
      String args = "(" + region + ") (" + summonerName + ")";
      StatsProfileCommandRunnable.executeCommand(server, event.getTextChannel(), args, new ArrayList<>(), null, event.getHook(), waiter, event.getMember(), false);
    }else {
      event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "statsProfileNeedParametersSlashCommand")).queue();
    }
  }
}
