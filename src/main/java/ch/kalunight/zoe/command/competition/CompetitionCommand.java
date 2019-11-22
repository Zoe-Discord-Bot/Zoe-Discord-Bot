package ch.kalunight.zoe.command.competition;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class CompetitionCommand extends ZoeCommand {

  public static final String USAGE_NAME = "competition";
  
  public CompetitionCommand() {
    this.name = USAGE_NAME;
    this.aliases = new String[] {"compet"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Command[] commandsChildren = {};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) {
    // TODO Auto-generated method stub
    
  }

}
