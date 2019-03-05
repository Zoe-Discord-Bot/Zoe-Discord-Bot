package ch.kalunight.zoe.command;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.service.GameChecker;
import net.dv8tion.jda.core.JDA.Status;

public class ShutDownCommand extends Command {

  private static final Logger logger = LoggerFactory.getLogger(ShutDownCommand.class);

  public ShutDownCommand() {
    this.name = "stop";
    this.help = "Safely shutdown the bot";
    this.hidden = true;
    this.ownerCommand = true;
    this.guildOnly = false;
  }

  @Override
  protected void execute(CommandEvent event) {
    
    event.reply("I shut down my self ...");

    GameChecker.setNeedToBeShutDown(true);
    
    while(!GameChecker.isShutdown()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    
    try {
      ServerData.shutDownTaskExecutor();
    } catch (InterruptedException e) {
      logger.error("Error in shutDownTaskExecutor : {}", e.getMessage(), e);
      Zoe.getJda().shutdownNow();
      try {
        Zoe.saveDataTxt();
      } catch (FileNotFoundException | UnsupportedEncodingException e1) {
        logger.error("La sauvegarde n'a pas pu être effectué !");
      }
      System.exit(1);
      Thread.currentThread().interrupt();
    }
    
    Zoe.getJda().shutdown();
    
    while(!Zoe.getJda().getStatus().equals(Status.SHUTDOWN)) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    
    try {
      Zoe.saveDataTxt();
    } catch(FileNotFoundException | UnsupportedEncodingException e) {
      logger.error("La sauvegarde n'a pas pu être effectué !");
    }
    
    System.exit(0);
  }
}
