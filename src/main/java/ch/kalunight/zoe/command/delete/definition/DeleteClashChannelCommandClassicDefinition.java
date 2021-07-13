package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.delete.DeleteClashChannelCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class DeleteClashChannelCommandClassicDefinition extends ZoeCommand {

  private EventWaiter eventWaiter;

  public DeleteClashChannelCommandClassicDefinition(EventWaiter waiter) {
    this.name = "clashChannel";
    String[] aliases = {"clash", "cc"};
    this.aliases = aliases;
    this.arguments = "";
    this.eventWaiter = waiter;
    this.help = "deleteClashChannelCommandHelpMessage";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Permission[] botPermissionRequiered = {Permission.MANAGE_CHANNEL, Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION};
    this.botPermissions = botPermissionRequiered;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeletePlayerCommandClassicDefinition.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    Message loadingMessage = event.getChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "loadingData")).complete();
    
    DeleteClashChannelCommandRunnable.executeCommand(server, eventWaiter, event.getMember(), loadingMessage, event.getTextChannel(), null);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
