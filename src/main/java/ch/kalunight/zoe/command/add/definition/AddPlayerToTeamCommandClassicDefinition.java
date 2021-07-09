package ch.kalunight.zoe.command.add.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.add.AddCommandRunnable;
import ch.kalunight.zoe.command.add.AddPlayerToTeamCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class AddPlayerToTeamCommandClassicDefinition extends ZoeCommand {

  public AddPlayerToTeamCommandClassicDefinition() {
    this.name = AddPlayerToTeamCommandRunnable.USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.arguments = "@MentionPlayer (teamName)";
    this.userPermissions = permissionRequired;
    this.help = "addPlayerToTeamCommandHelp";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(AddCommandRunnable.USAGE_NAME, AddPlayerToTeamCommandRunnable.USAGE_NAME, arguments, help);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    String message = AddPlayerToTeamCommandRunnable.executeCommand(server, event.getMessage().getMentionedMembers(), event.getArgs());
    
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
