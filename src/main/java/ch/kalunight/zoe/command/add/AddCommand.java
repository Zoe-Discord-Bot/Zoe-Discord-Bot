package ch.kalunight.zoe.command.add;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;

public class AddCommand extends Command {

  public AddCommand() {
    this.name = "add";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about add commands";
    Command[] commandsChildren = {new AddPlayerToTeam()};
    this.children = commandsChildren;
  }
  
  @Override
  protected void execute(CommandEvent event) {
    
  }

}
