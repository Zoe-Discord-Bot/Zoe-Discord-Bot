package ch.kalunight.zoe.command.helpus;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.CommandUtil;

public class HelpUsCommand extends Command {

  public HelpUsCommand() {
    this.name = "helpus";
    this.help = "Send info about helpUs command.";
    Command[] commandsChildren = {new HelpUsCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    event.reply("If you need help for helpUs commands, type `>helpus help`");
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HelpUs commands :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + name + " " + commandChildren.getName() + " " + commandChildren.getArguments()
          + "` : " + commandChildren.getHelp() + "\n");
        }
        
        event.reply(stringBuilder.toString());
      }
    };
  }

}
