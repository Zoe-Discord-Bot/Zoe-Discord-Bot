package ch.kalunight.zoe.command;

import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;

public class PatchNotesCommandRunnable {
  
  private PatchNotesCommandRunnable() {
    // hide default constructor
  }
  
  public static String executeCommand(Server server) {
    return LanguageManager.getText(server.getLanguage(), "patchNoteCommandRespondMessage");
  }
}
