package ch.kalunight.zoe.command.add;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class AddCommand extends Command {

  public static final String USAGE_NAME = "add";

  public AddCommand() {
    this.name = USAGE_NAME;
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Command[] commandsChildren = {new AddPlayerToTeamCommand(), new AddAccountCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }

  @Override
  protected void execute(CommandEvent event) {
    event.reply("If you need help for add commands, type `>add help`");
  }
}
