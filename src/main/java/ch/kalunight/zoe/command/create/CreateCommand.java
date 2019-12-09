package ch.kalunight.zoe.command.create;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class CreateCommand extends ZoeCommand {

  public static final String USAGE_NAME = "create";
  
  public CreateCommand() {
    this.name = USAGE_NAME;
    this.aliases = new String[] {"c"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Command[] commandsChildren = {new CreateInfoChannelCommand(), new CreatePlayerCommand(), new CreateTeamCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.reply(LanguageManager.getText(server.serv_language, "mainCreateCommandHelpMessage"));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
