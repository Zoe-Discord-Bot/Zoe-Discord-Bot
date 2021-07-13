package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ConfigCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class ConfigCommandClassicDefinition extends ZoeCommand {

  private EventWaiter waiter;
  
  public ConfigCommandClassicDefinition(EventWaiter waiter) {
    this.name = "config";
    this.help = "configCommandHelp";
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL, Permission.MESSAGE_ADD_REACTION};
    this.userPermissions = permissionRequired;
    Permission[] permissionBot = {Permission.MESSAGE_ADD_REACTION, Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS};
    this.botPermissions = permissionBot;
    this.guildOnly = true;
    this.waiter = waiter;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    
    Server server = getServer(event.getGuild().getIdLong());
    
    CommandGuildDiscordData data = new CommandGuildDiscordData(event.getMember(), event.getGuild(), event.getTextChannel());
    
    ConfigCommandRunnable.executeCommand(server, waiter, data);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
