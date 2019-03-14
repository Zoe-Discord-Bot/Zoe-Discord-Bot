package ch.kalunight.zoe.command.add;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.core.Permission;

public class AddCommand extends Command {

  public AddCommand() {
    this.name = "add";
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about add commands";
    Command[] commandsChildren = {new AddPlayerToTeam()};
    this.children = commandsChildren;
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        event.getTextChannel().sendTyping().complete();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Add commands :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("`>" + name + " " + commandChildren.getName() + " " + commandChildren.getArguments() + "` : " + commandChildren.getHelp());
        }
        
        event.reply(stringBuilder.toString());
      }
    };
  }
}
