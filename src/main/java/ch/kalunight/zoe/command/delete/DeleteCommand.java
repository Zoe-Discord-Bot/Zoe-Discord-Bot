package ch.kalunight.zoe.command.delete;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.CommandUtil;
import net.dv8tion.jda.core.Permission;

public class DeleteCommand extends Command {

  public DeleteCommand() {
    this.name = "delete";
    this.aliases = new String[] {"d"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about delete command.";
    Command[] commandsChildren = {new DeletePlayerCommand(), new DeleteInfoChannelCommand(), new DeleteTeamCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    event.reply("If you need help for delete commands, type `>delete help`");
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Delete commands :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + name + " " + commandChildren.getName() + " " + commandChildren.getArguments()
          + "` : " + commandChildren.getHelp() + "\n");
        }
        
        event.reply(stringBuilder.toString());
      }
    };
  }
}
