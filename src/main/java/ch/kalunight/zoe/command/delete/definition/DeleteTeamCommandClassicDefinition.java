package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.delete.DeletePlayerRunnable;
import ch.kalunight.zoe.command.delete.DeleteTeamCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class DeleteTeamCommandClassicDefinition extends ZoeCommand {
  
  public DeleteTeamCommandClassicDefinition() {
    this.name = DeleteTeamCommandRunnable.USAGE_NAME;
    this.help = "deleteTeamHelpMessage";
    this.arguments = "teamName";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeletePlayerRunnable.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String message = DeleteTeamCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()), event.getArgs());
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
