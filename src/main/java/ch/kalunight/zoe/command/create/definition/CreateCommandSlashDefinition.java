package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.create.CreateCommandRunnable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class CreateCommandSlashDefinition extends ZoeSlashCommand {

  public CreateCommandSlashDefinition(EventWaiter waiter, String serverId) {
    this.name = CreateCommandClassicDefinition.USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    SlashCommand[] commandsChildren = {new CreatePlayerCommandSlashDefinition(waiter, serverId), new CreateInfochannelCommandSlashDefinition(serverId), new CreateTeamCommandSlashDefinition(serverId),
        new CreateRankHistoryChannelCommandSlashDefinition(serverId), new CreateLeaderboardCommandSlashDefinition(waiter, serverId), new CreateClashChannelCommandSlashDefinition(waiter, serverId)};
    this.children = commandsChildren;
    
    if(serverId == null) {
      this.guildOnly = true;
    }else {
      this.guildOnly = true; //True for testing
      this.guildId = serverId; //Test server
    }
  }

  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    String message = CreateCommandRunnable.executeCommand(ZoeCommand.getLanguage(event.getGuild()));
    event.getHook().editOriginal(message).queue();
  }
  
}
