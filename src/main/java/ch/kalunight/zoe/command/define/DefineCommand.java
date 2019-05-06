package ch.kalunight.zoe.command.define;

import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.CommandUtil;
import net.dv8tion.jda.core.Permission;

public class DefineCommand extends Command {

  public DefineCommand() {
    this.name = "define";
    this.aliases = new String[] {"def"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about define commands";
    Command[] commandsChildren = {new DefineInfoChannelCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    event.reply("If you need help for define command, type `>define help`");
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Define command :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + name + " " + commandChildren.getName() + " " + commandChildren.getArguments() + "` : "
              + commandChildren.getHelp() + "\n");
        }

        event.reply(stringBuilder.toString());
      }
    };
  }
}
