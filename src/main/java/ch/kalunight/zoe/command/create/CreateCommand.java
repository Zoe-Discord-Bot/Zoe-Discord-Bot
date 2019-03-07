package ch.kalunight.zoe.command.create;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;

public class CreateCommand extends Command {

  public CreateCommand() {
    this.name = "create";
    this.aliases = new String[] {"c"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about create commands";
    Command[] commandsChildren = {new CreateInfoChannelCommand(), new CreatePlayerCommand()};
    this.children = commandsChildren;
  }
  
  @Override
  protected void execute(CommandEvent event) {
    //TODO Write info

  }

}
