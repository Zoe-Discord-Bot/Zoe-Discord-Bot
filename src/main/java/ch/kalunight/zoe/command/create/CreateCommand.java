package ch.kalunight.zoe.command.create;

import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.CommandUtil;
import net.dv8tion.jda.core.Permission;

public class CreateCommand extends Command {

  public static final String USAGE_NAME = "create";
  
  public CreateCommand() {
    this.name = USAGE_NAME;
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
    event.reply("If you need help for create commands, type `>create help`");
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Create commands :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + name + " " + commandChildren.getName() + " " + commandChildren.getArguments() + "` : "
              + commandChildren.getHelp() + "\n");
        }

        event.reply(stringBuilder.toString());
      }
    };
  }
}
