package ch.kalunight.zoe.command.delete.definition;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.delete.DeleteCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class DeleteCommandSlashCommand extends ZoeSlashCommand {

  public DeleteCommandSlashCommand(EventWaiter waiter, String serverId) {
    this.name = DeleteCommandClassicDefinition.USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    SlashCommand[] commandsChildren = {new DeletePlayerCommandSlashDefinition(serverId), new DeleteTeamCommandSlashDefinition(serverId), new DeleteInfoChannelCommandSlashDefinition(serverId),
        new DeleteClashChannelCommandSlashDefinition(waiter, serverId), new DeleteLeaderboardCommandSlashDefinition(waiter, serverId), new DeleteRankHistoryChannelCommandSlashDefinition(serverId)};
    this.children = commandsChildren;
    
    if(serverId == null) {
      this.guildOnly = true;
    }else {
      this.guildOnly = true; //True for testing
      this.guildId = serverId; //Test server
    }
  }

  @Override
  protected void executeCommand(SlashCommandEvent event) {
    DTO.Server server = ZoeCommand.getServer(event.getGuild().getIdLong());
    
    String message = DeleteCommandRunnable.executeCommand(server);
    
    event.getHook().editOriginal(message).queue();
  }
  
}
