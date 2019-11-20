package ch.kalunight.zoe.command.admin;

import java.io.IOException;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;

public class RefreshLanguageCommand extends ZoeCommand {

  private static final Logger logger = LoggerFactory.getLogger(RefreshLanguageCommand.class);

  public RefreshLanguageCommand() {
    this.name = "refreshLanguage";
    this.arguments = "";
    this.help = "Refresh file language";
    this.ownerCommand = true;
    this.hidden = true;
    this.helpBiConsumer = getHelpMethod();
  }

  
  @Override
  protected void executeCommand(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    event.reply("Refresh translation ...");
    try {
      LanguageManager.loadTranslations();
    } catch(IOException e) {
      event.reply("Exception throw when loading translation ! Error : " + e.getMessage());
      logger.error("Exception when loading new translation", e);
      return;
    }
    event.reply("Done !");
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Admin RefreshLanguageCommand command :\n");
        stringBuilder.append("--> `>admin " + name + " " + arguments + "` : " + help);
        event.reply(stringBuilder.toString());
      }
    };
  }

}
