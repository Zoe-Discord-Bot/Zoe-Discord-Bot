package ch.kalunight.zoe.command;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.static_data.SpellingLanguage;

public abstract class ZoeCommand extends Command {

  private static final AtomicInteger commandExecuted = new AtomicInteger(0);
  private static final AtomicInteger commandFinishedCorrectly = new AtomicInteger(0);
  private static final AtomicInteger commandFinishedWithError = new AtomicInteger(0);

  private static final Logger logger = LoggerFactory.getLogger(ZoeCommand.class);
  
  @Override
  protected void execute(CommandEvent event) {
    logger.info("Command \"{}\" executed", this.getClass().getName());
    commandExecuted.incrementAndGet();
    
    Server server = ServerData.getServers().get(event.getGuild().getId());

    if(server == null) {
      server = new Server(event.getGuild().getIdLong(), SpellingLanguage.EN, new ServerConfiguration());
      ServerData.getServers().put(event.getGuild().getId(), server);
    }
    
    try {
      executeCommand(event);
    } catch (Exception e) {
      logger.error("Unexpected exception in {} commands. Error : {}", this.getClass().getName(), e.getMessage(), e);
      commandFinishedWithError.incrementAndGet();
      return;
    }
    logger.info("Command \"{}\" finished correctly", this.getClass().getName());
    commandFinishedCorrectly.incrementAndGet();
  }
  
  protected abstract void executeCommand(CommandEvent event);

  public static void clearStats() {
    commandExecuted.set(0);
    commandFinishedCorrectly.set(0);
    commandFinishedWithError.set(0);
  }
  
  public static AtomicInteger getCommandExecuted() {
    return commandExecuted;
  }

  public static AtomicInteger getCommandFinishedCorrectly() {
    return commandFinishedCorrectly;
  }

  public static AtomicInteger getCommandFinishedWithError() {
    return commandFinishedWithError;
  }

}
