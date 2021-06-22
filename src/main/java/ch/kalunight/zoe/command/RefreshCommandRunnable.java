package ch.kalunight.zoe.command;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;

public class RefreshCommandRunnable {

  private RefreshCommandRunnable() {
    //hide default public constructor
  }

  public static String executeCommand(Server server) {
    ServerThreadsManager.getServersAskedTreatment().add(server);
    return LanguageManager.getText(server.getLanguage(), "refreshCommandDoneMessage");
  }
}
