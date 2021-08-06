package ch.kalunight.zoe.command;

import ch.kalunight.zoe.translation.LanguageManager;

public class WikiCommandRunnable {

  private WikiCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(String language) {
    return LanguageManager.getText(language, "wikiCommandResponse");
  }
}
