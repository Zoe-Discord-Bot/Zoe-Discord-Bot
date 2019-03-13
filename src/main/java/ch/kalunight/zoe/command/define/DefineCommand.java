package ch.kalunight.zoe.command.define;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;

public class DefineCommand extends Command{

  public DefineCommand() {
    this.name = "define";
    this.aliases = new String[] {"def"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about define commands";
    Command[] commandsChildren = {new DefineInfoChannelCommand()};
    this.children = commandsChildren;
  }
  
  @Override
  protected void execute(CommandEvent event) {
    // TODO: Implement info
  }

}
