package ch.kalunight.zoe.command.remove;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.CommandUtil;

public class RemoveCommand extends Command {

  public static final String USAGE_NAME = "remove";

  public RemoveCommand() {
    this.name = USAGE_NAME;
    this.help = "Send info about remove commands";
    Command[] commandsChildren = {new RemovePlayerToTeamCommand(), new RemoveAccountCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }

  @Override
  protected void execute(CommandEvent event) {
    event.reply("If you need help for remove command, type `>remove help`");
  }
}
