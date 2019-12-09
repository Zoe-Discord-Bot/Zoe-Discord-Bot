package ch.kalunight.zoe.command;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;

public class SetupCommand extends ZoeCommand {

  public SetupCommand() {
    this.name = "setup";
    this.help = "setupHelpMessage";
    this.ownerCommand = false;
    this.guildOnly = false;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    
    event.reply(LanguageManager.getText(server.serv_language, "setupMessage"));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
