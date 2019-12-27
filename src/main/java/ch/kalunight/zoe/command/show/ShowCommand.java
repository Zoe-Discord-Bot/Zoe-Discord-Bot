package ch.kalunight.zoe.command.show;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class ShowCommand extends ZoeCommand {
  
  public static final String USAGE_NAME = "show";

  public ShowCommand(EventWaiter waiter) {
    this.name = USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Command[] commandsChildren = {new ShowPlayerCommand(waiter)};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    DTO.Server server = getServer(event.getGuild().getIdLong());
    event.reply(LanguageManager.getText(server.serv_language, "mainShowCommandHelpMessage"));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
