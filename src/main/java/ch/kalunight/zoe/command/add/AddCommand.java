package ch.kalunight.zoe.command.add;

import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.core.Permission;

public class AddCommand extends Command {

  private static final Logger logger = LoggerFactory.getLogger(AddCommand.class);
  
  public AddCommand() {
    this.name = "add";
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Send info about add commands";
    Command[] commandsChildren = {new AddPlayerToTeam()};
    this.children = commandsChildren;
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    event.reply("If you need help for add commands, type `>add help`");
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        switch(event.getChannelType()) {
        case PRIVATE:
          event.getPrivateChannel().sendTyping().complete();
          break;
        case TEXT:
          event.getTextChannel().sendTyping().complete();
          break;
        default:
          logger.warn("event.getChannelType() return a unexpected type : " + event.getChannelType().toString());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Add command :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + name + " " + commandChildren.getName() + " " + commandChildren.getArguments() + "` : " + commandChildren.getHelp());
        }
        
        event.reply(stringBuilder.toString());
      }
    };
  }
}
