package ch.kalunight.zoe.command.show.definition;

import java.sql.SQLException;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.show.ShowCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class ShowCommandSlashDefinition extends ZoeSlashCommand {
  
  public ShowCommandSlashDefinition(EventWaiter waiter, String serverId) {
    this.name = ShowCommandRunnable.USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    SlashCommand[] commandsChildren = {new ShowPlayerCommandSlashDefinition(waiter, serverId)};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(ShowCommandRunnable.USAGE_NAME, commandsChildren);
  }

  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    String message = ShowCommandRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()));
    
    event.getHook().editOriginal(message).queue();
  }

}
