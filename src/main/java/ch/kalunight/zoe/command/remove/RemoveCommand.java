package ch.kalunight.zoe.command.remove;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.util.CommandUtil;

public class RemoveCommand extends ZoeCommand {

  public static final String USAGE_NAME = "remove";

  public RemoveCommand() {
    this.name = USAGE_NAME;
    this.help = "Send info about remove commands";
    Command[] commandsChildren = {new RemovePlayerToTeamCommand(), new RemoveAccountCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.reply("If you need help for remove command, type `>remove help`");
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Remove command :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + name + " " + commandChildren.getName() + " " + commandChildren.getArguments() + "` : "
              + commandChildren.getHelp() + "\n");
        }

        event.reply(stringBuilder.toString());
      }
    };
  }

}
