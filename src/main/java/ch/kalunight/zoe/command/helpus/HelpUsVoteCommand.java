package ch.kalunight.zoe.command.helpus;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.CommandUtil;

public class HelpUsVoteCommand extends Command {

  public HelpUsVoteCommand() {
    this.name = "vote";
    this.help = "Add an upvote for me on https://discordbots.org/. Vote in weekend will count double ! Thanks you in advance :D";
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HelpUs vote command :\n");
        stringBuilder.append("--> `>helpus " + name + " " + arguments + "` : " + help);
        
        event.reply(stringBuilder.toString());
      }
    };
  }

}
