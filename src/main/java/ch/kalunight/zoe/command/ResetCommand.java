package ch.kalunight.zoe.command;

import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.static_data.SpellingLanguage;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ResetCommand extends ZoeCommand {
  
  private final EventWaiter waiter;

  public ResetCommand(EventWaiter waiter) {
    this.name = "reset";
    this.help = "resetCommandHelp";
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.waiter = waiter;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    event.reply(LanguageManager.getText(server.getLangage(), "resetWarningMessage"));
    
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
        e -> reset(e), 1, TimeUnit.MINUTES,
        () -> cancelReset(event.getEvent()));
  }

  private void reset(MessageReceivedEvent messageReceivedEvent) {
    Server server = ServerData.getServers().get(messageReceivedEvent.getGuild().getId());
    
    SpellingLanguage spellingLangage = SpellingLanguage.EN;
    
    if(server != null) {
      spellingLangage = server.getLangage();
    }
    
    if(messageReceivedEvent.getMessage().getContentRaw().equals("YES")) {
      
      messageReceivedEvent.getTextChannel().sendMessage(LanguageManager.getText(spellingLangage, "resetConfirmationMessage")).queue();
      
      ServerData.getServers().put(messageReceivedEvent.getGuild().getId(), 
          new Server(messageReceivedEvent.getGuild().getIdLong(), spellingLangage, new ServerConfiguration()));
      
      messageReceivedEvent.getTextChannel().sendMessage(LanguageManager.getText(spellingLangage, "resetDoneMessage")).queue();
    }else {
      messageReceivedEvent.getTextChannel().sendMessage(LanguageManager.getText(spellingLangage, "resetCancelMessage")).queue();
    }
  }
  
  private void cancelReset(MessageReceivedEvent event) {
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    SpellingLanguage spellingLangage = SpellingLanguage.EN;
    
    if(server != null) {
      spellingLangage = server.getLangage();
    }
    event.getTextChannel().sendMessage(LanguageManager.getText(spellingLangage, "resetTimeoutMessage")).queue();
  }
}
