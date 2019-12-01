package ch.kalunight.zoe.command.stats;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
  
public class StatsCommand extends ZoeCommand {

  public static final String USAGE_NAME = "stats";

  public StatsCommand(EventWaiter waiter) {
    this.name = USAGE_NAME;
    this.aliases = new String[] {"s"};
    Command[] commandsChildren = {new StatsProfileCommand(waiter)};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) {
    if(!event.getMessage().getMentionedMembers().isEmpty()) {
      for(Command command : Zoe.getMainCommands(null)) {
        for(Command commandChild : command.getChildren()) {
          if(commandChild instanceof StatsProfileCommand) {
            ((StatsProfileCommand) commandChild).executeCommand(event);
            return;
          }
        }
      }
    }
    
    event.reply(LanguageManager.getText(ServerData.getServers().get(event.getGuild().getId()).getLangage(), "mainStatsCommandHelpMessage"));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
