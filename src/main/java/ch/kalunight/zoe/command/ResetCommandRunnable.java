package ch.kalunight.zoe.command;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class ResetCommandRunnable {
  
  private ResetCommandRunnable() {
    // hide default constructor
  }
  
  public static void executeCommand(Server server, TextChannel channel, InteractionHook hook, EventWaiter waiter, User user) {
    
    String message = LanguageManager.getText(server.getLanguage(), "resetWarningMessage");
    
    if(hook != null) {
      hook.editOriginal(message).queue();
    }else {
      channel.sendMessage(message).queue();
    }
    
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(user) && e.getChannel().equals(channel),
        e -> reset(e, server), 1, TimeUnit.MINUTES,
        () -> cancelReset(server, channel));
  }

  private static void reset(MessageReceivedEvent messageReceivedEvent, Server server) {
    String spellingLangage = LanguageManager.DEFAULT_LANGUAGE;
    
    if(server != null) {
      spellingLangage = server.getLanguage();
    }
    
    if(messageReceivedEvent.getMessage().getContentRaw().equals("YES")) {
      
      messageReceivedEvent.getTextChannel().sendMessage(LanguageManager.getText(spellingLangage, "resetConfirmationMessage")).queue();
      
      try {
        ServerRepository.deleteServer(messageReceivedEvent.getGuild().getIdLong());
        ServerRepository.createNewServer(messageReceivedEvent.getGuild().getIdLong(), spellingLangage);
      } catch (SQLException e) {
        RepoRessources.sqlErrorReport(messageReceivedEvent.getChannel(), server, e);
        return;
      }
      
      messageReceivedEvent.getTextChannel().sendMessage(LanguageManager.getText(spellingLangage, "resetDoneMessage")).queue();
    }else {
      messageReceivedEvent.getTextChannel().sendMessage(LanguageManager.getText(spellingLangage, "resetCancelMessage")).queue();
    }
  }
  
  private static void cancelReset(Server server, TextChannel channel) {
    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "resetTimeoutMessage")).queue();
  }
}
