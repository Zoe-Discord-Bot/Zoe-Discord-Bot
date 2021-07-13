package ch.kalunight.zoe.command.remove;

import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;

public class RemoveCommandRunnable {

  public static final String USAGE_NAME = "remove";

  private RemoveCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(Server server) {
    return LanguageManager.getText(server.getLanguage(), "mainRemoveCommandHelpMessage");
  }
}
