package ch.kalunight.zoe.command.stats;

import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.CommandUtil;

public class StatsCommand extends Command {
  
  public static final String USAGE_NAME = "stats";

  public StatsCommand(EventWaiter waiter) {
    this.name = USAGE_NAME;
    this.aliases = new String[] {"s"};
    this.help = "Send info about stats command.";
    Command[] commandsChildren = {new StatsProfileCommand(waiter)};
    this.children = commandsChildren;
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    if(!event.getMessage().getMentionedMembers().isEmpty()) {
      for(Command command : Zoe.getMainCommands(null)) {
        for(Command commandChild : command.getChildren()) {
          if(commandChild instanceof StatsProfileCommand) {
            ((StatsProfileCommand) commandChild).execute(event);
            return;
          }
        }
      }
    }
    
    event.reply("If you want help about stats commands, type `>stats help`");
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Stats commands :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + name + " " + commandChildren.getName() + " " + commandChildren.getArguments()
          + "` : " + commandChildren.getHelp() + "\n");
        }
        
        event.reply(stringBuilder.toString());
      }
    };
  }
}
