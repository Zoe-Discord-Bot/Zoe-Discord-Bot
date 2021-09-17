package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.create.CreateClashChannelRunnable;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CreateClashChannelCommandSlashDefinition extends ZoeSlashCommand {

  private EventWaiter waiter;
  
  public CreateClashChannelCommandSlashDefinition(EventWaiter event, String serverId) {
    this.name = "clashchannel";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Permission[] botPermissionRequiered = {Permission.MANAGE_CHANNEL, Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION};
    this.botPermissions = botPermissionRequiered;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "createClashChannelHelpMessageSlashCommand");
    this.waiter = event;
    
    List<OptionData> data = new ArrayList<>();
    
    OptionData infochannelName = new OptionData(OptionType.STRING, "clashchannel-name", "The name for the clashchannel.");
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
    CreateClashChannelRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()), event.getOption("clashchannel-name").getAsString(),
        waiter, event.getMember(), null, event.getTextChannel(), event.getHook(), true);
  }
  
}
