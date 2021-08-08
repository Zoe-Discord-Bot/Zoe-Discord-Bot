package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.CreateClashChannelRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class CreateClashChannelCommandClassicDefinition extends ZoeCommand{

  private EventWaiter waiter;
  
  public CreateClashChannelCommandClassicDefinition(EventWaiter event) {
    this.name = "clashChannel";
    this.arguments = "nameOfTheNewChannel";
    String[] aliases = {"clash", "cc"};
    this.aliases = aliases;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Permission[] botPermissionRequiered = {Permission.MANAGE_CHANNEL, Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION};
    this.botPermissions = botPermissionRequiered;
    this.guildOnly = true;
    this.help = "createClashChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommandClassicDefinition.USAGE_NAME, name, arguments, help);
    this.waiter = event;
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    Message message = event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "loadingData")).complete();
    
    CreateClashChannelRunnable.executeCommand(server, event.getArgs(), waiter, event.getMember(), message, event.getTextChannel(), null);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
