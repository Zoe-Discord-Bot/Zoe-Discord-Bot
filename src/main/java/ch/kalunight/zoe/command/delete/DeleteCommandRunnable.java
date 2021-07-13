package ch.kalunight.zoe.command.delete;

import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;

public class DeleteCommandRunnable {
  
  private DeleteCommandRunnable() {
    // hide default public constructor
  }

  public static String executeCommand(Server server) {
    return LanguageManager.getText(server.getLanguage(), "mainDeleteCommandHelpMessage");
  }

}
