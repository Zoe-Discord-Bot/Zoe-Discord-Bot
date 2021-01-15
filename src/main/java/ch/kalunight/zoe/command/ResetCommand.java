package ch.kalunight.zoe.command;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.repositories.ServerRepository;
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
    
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    event.reply(LanguageManager.getText(server.getLanguage(), "resetWarningMessage"));
    
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
        e -> reset(e), 1, TimeUnit.MINUTES,
        () -> cancelReset(event.getEvent()));
  }

  private void reset(MessageReceivedEvent messageReceivedEvent) {
    String spellingLangage = LanguageManager.DEFAULT_LANGUAGE;
    
    DTO.Server server = getServer(messageReceivedEvent.getGuild().getIdLong());
    
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
  
  private void cancelReset(MessageReceivedEvent event) {
    DTO.Server server = getServer(event.getGuild().getIdLong());
    event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "resetTimeoutMessage")).queue();
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
