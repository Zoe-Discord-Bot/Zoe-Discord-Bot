package ch.kalunight.zoe.command.admin;

import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.util.CommandUtil;

public class AdminCommand extends ZoeCommand {

  public AdminCommand() {
    this.name = "admin";
    this.ownerCommand = true;
    this.hidden = true;
    this.help = "Send info about admin commands";
    Command[] commandsChildren = {new AdminSendAnnonceMessageCommand(), new AdminCreateRAPIChannel(), new AdminDeleteRAPIChannel(),
        new RefreshLanguageCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void executeCommand(CommandEvent event) {
    event.reply("Admins command, type `>admin help` for help");
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Admins commands :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + name + " " + commandChildren.getName() + " " + commandChildren.getArguments() + "` : "
              + commandChildren.getHelp() + "\n");
        }
        event.reply(stringBuilder.toString());
      }
    };
  }
}
