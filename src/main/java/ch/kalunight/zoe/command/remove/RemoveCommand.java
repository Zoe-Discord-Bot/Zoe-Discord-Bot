package ch.kalunight.zoe.command.remove;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;

public class RemoveCommand extends ZoeCommand {

  public static final String USAGE_NAME = "remove";

  public RemoveCommand() {
    this.name = USAGE_NAME;
    Command[] commandsChildren = {new RemovePlayerToTeamCommand(), new RemoveAccountCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.reply(LanguageManager.getText(getServer(event.getGuild().getIdLong()).serv_language, "mainRemoveCommandHelpMessage"));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
