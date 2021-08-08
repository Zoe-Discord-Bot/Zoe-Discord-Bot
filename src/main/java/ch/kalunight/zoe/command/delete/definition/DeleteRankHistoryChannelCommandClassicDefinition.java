package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.delete.DeleteRankHistoryChannelCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class DeleteRankHistoryChannelCommandClassicDefinition extends ZoeCommand {

  public DeleteRankHistoryChannelCommandClassicDefinition() {
    this.name = "rankChannel";
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.help = "deleteRankChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeletePlayerCommandClassicDefinition.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    event.reply(DeleteRankHistoryChannelCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()), event.getGuild()));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
