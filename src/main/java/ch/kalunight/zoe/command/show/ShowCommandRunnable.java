package ch.kalunight.zoe.command.show;

import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;

public class ShowCommandRunnable {
  
  public static final String USAGE_NAME = "show";
  
  private ShowCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(Server server) {
    return LanguageManager.getText(server.getLanguage(), "mainShowCommandHelpMessage");
  }
}
