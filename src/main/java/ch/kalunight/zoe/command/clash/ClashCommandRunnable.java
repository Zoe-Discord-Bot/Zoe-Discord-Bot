package ch.kalunight.zoe.command.clash;

import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.TextChannel;

public class ClashCommandRunnable {

  public static final String USAGE_NAME = "clash";

  private ClashCommandRunnable() {
    // hide default public constructor
  }
  
  public static void executeCommand(Server server, TextChannel channel) {
    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "mainClashCommandHelpMessage")).queue();;
  }

}
