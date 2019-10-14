package ch.kalunight.zoe.command.define;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class DefineCommand extends Command {

  public static final String USAGE_NAME = "define";
  
  public DefineCommand() {
    this.name = USAGE_NAME;
    this.aliases = new String[] {"def"};
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Command[] commandsChildren = {new DefineInfoChannelCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }

  @Override
  protected void execute(CommandEvent event) {
    event.reply("If you need help for define command, type `>define help`");
  }
}
