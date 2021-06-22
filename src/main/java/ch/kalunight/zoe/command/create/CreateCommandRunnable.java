package ch.kalunight.zoe.command.create;

import ch.kalunight.zoe.translation.LanguageManager;

public class CreateCommandRunnable {

  private CreateCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(String language) {
    return LanguageManager.getText(language, "mainCreateCommandHelpMessage");
  }
}
