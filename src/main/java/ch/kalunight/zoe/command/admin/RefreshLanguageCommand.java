package ch.kalunight.zoe.command.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildrenNoTranslation(AdminCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    event.reply("Refresh translation ...");
    try {
      LanguageManager.loadTranslations();
    } catch(Exception e) {
      event.reply("Exception throw when loading translation ! Error : " + e.getMessage());
      logger.error("Exception when loading new translation", e);
      return;
    }
    event.reply("Done !");
  }
}
