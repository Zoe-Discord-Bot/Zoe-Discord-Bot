package ch.kalunight.zoe.command.delete;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.core.Permission;

public class DeleteCommand extends Command {

  public DeleteCommand() {
    this.name = "delete";
    this.aliases = new String[] {"d"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about delete Command";
    Command[] commandsChildren = {new DeletePlayerCommand(), new DeleteInfoChannelCommand()};
    this.children = commandsChildren;
  }
  
  @Override
  protected void execute(CommandEvent event) {
    // TODO Give info about Delete Command

  }

}
