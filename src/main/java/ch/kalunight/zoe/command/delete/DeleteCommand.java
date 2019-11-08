package ch.kalunight.zoe.command.delete;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class DeleteCommand extends ZoeCommand {

  public static final String USAGE_NAME = "delete";

  public DeleteCommand() {
    this.name = USAGE_NAME;
    this.aliases = new String[] {"d"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about delete command.";
    Command[] commandsChildren = {new DeletePlayerCommand(), new DeleteInfoChannelCommand(), new DeleteTeamCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.reply("If you need help for delete commands, type `>delete help`");
  }
}
