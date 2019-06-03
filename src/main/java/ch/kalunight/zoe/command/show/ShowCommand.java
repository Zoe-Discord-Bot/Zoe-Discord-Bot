package ch.kalunight.zoe.command.show;

import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.command.CommandUtil;
import net.dv8tion.jda.core.Permission;

public class ShowCommand extends Command {
  
  public static final String USAGE_NAME = "show";

  public ShowCommand(EventWaiter waiter) {
    this.name = USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about show commands";
    Command[] commandsChildren = {new ShowPlayerCommand(waiter)};
    this.children = commandsChildren;
    this.helpBiConsumer = getHelpMethod();
  }

  
  @Override
  protected void execute(CommandEvent event) {
    event.reply("If you need help for show command, type `>remove help`");
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Show command :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + name + " " + commandChildren.getName() + " " + commandChildren.getArguments() + "` : "
              + commandChildren.getHelp() + "\n");
        }

        event.reply(stringBuilder.toString());
      }
    };
  }
  
}
