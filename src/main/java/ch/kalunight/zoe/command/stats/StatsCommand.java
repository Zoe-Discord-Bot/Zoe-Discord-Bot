package ch.kalunight.zoe.command.stats;

import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.CommandUtil;

public class StatsCommand extends Command {

  public StatsCommand() {
    this.name = "stats";
    this.aliases = new String[] {"s"};
    this.help = "Send info about stats command.";
    Command[] commandsChildren = {new StatsProfileCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
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
