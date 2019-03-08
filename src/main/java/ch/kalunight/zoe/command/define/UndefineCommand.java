package ch.kalunight.zoe.command.define;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;

public class UndefineCommand extends Command {

  public UndefineCommand() {
    this.name = "undefine";
    this.aliases = new String[] {"undef"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about undefine commands";
    Command[] commandsChildren = {new UndefineInfoChannelCommand()};
    this.children = commandsChildren;
  }
  
  @Override
  protected void execute(CommandEvent event) {
    //TODO: send info about undefine command
    
  }

}
