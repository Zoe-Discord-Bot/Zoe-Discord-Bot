package ch.kalunight.zoe.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public abstract class ZoeCommand extends Command {

  private static final Logger logger = LoggerFactory.getLogger(ZoeCommand.class);
  
  @Override
  protected void execute(CommandEvent event) {
    logger.info("Command \"{}\" executed", this.getClass().getName());
    try {
      executeCommand(event);
    } catch (Exception e) {
      logger.error("Unexpected exception in {} commands. Error : {}", this.getClass().getName(), e.getMessage(), e);
      return;
    }
    logger.info("Command \"{}\" finished correctly", this.getClass().getName());
  }
  
  protected abstract void executeCommand(CommandEvent event);

}
