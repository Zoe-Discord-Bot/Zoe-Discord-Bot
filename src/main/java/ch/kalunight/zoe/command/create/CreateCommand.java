package ch.kalunight.zoe.command.create;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.CommandUtil;
import net.dv8tion.jda.core.Permission;

public class CreateCommand extends Command {

  public CreateCommand() {
    this.name = "create";
    this.aliases = new String[] {"c"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about create commands";
    Command[] commandsChildren = {new CreateInfoChannelCommand(), new CreatePlayerCommand(), new CreateTeamCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    //TODO Write info

  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Create commands :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + name + " " + commandChildren.getName() + " " + commandChildren.getArguments()
          + "` : " + commandChildren.getHelp() + "\n");
        }
        
        stringBuilder.append("**NOTE : All these commands need the permission to manage channel !**");
        event.reply(stringBuilder.toString());
      }
    };
  }
}
