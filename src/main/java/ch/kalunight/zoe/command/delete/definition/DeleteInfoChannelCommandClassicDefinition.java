package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.delete.DeleteInfoChannelCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class DeleteInfoChannelCommandClassicDefinition extends ZoeCommand {

  public DeleteInfoChannelCommandClassicDefinition() {
    this.name = "infoChannel";
    this.arguments = "";
    this.help = "deleteInfoChannelHelpMessage";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeletePlayerCommandClassicDefinition.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String message = DeleteInfoChannelCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()), event.getGuild());
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
