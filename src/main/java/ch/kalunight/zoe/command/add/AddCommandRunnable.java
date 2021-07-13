package ch.kalunight.zoe.command.add;

import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;

public class AddCommandRunnable {

  public static final String USAGE_NAME = "add";

  private AddCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(Server server) {
    return LanguageManager.getText(server.getLanguage(), "mainAddCommandHelpMessage");
  }
}
