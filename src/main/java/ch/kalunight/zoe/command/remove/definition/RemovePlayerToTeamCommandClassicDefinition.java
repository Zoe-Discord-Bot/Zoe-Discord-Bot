package ch.kalunight.zoe.command.remove.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.remove.RemoveCommandRunnable;
import ch.kalunight.zoe.command.remove.RemovePlayerToTeamCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class RemovePlayerToTeamCommandClassicDefinition extends ZoeCommand {

  public RemovePlayerToTeamCommandClassicDefinition() {
    this.name = RemovePlayerToTeamCommandRunnable.USAGE_NAME;
    this.help = "removePlayerToTeamHelpMessage";
    this.arguments = "@MentionOfPlayer";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(RemoveCommandRunnable.USAGE_NAME, name, arguments, help);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    String message = RemovePlayerToTeamCommandRunnable.executeCommand(server, event.getMessage().getMentionedMembers(), event.getJDA());
    
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
