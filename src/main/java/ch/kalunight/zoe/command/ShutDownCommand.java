package ch.kalunight.zoe.command;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import net.dv8tion.jda.core.JDA.Status;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Game.GameType;

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
    TextChannel channel = event.getTextChannel();

    if(channel == null) {
      event.reply("Please send this command in a guild text channel !");
      return;
    }
    
    channel.sendTyping().complete();

    logger.info("Shutdown started !");
    
    channel.sendMessage("ShutDown Started ! I will not respond anymore ...").complete();

    for(Object eventListener : Zoe.getEventlistenerlist()) {
      Zoe.getJda().removeEventListener(eventListener);
    }
    
    Zoe.getJda().getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Game.of(GameType.DEFAULT, "Shuting down ..."));
    
    ServerData.getServerCheckerThreadTimer().cancel();

    logger.info("The server checker thread has been safely stopped ...");
    channel.sendMessage("The server checker thread has been safely stopped ...").complete();

    try {
      ServerData.shutDownTaskExecutor(channel);
    } catch(InterruptedException e) {
      event.reply("Thread got interupted ! Save end shut down without finish task : " + e.getMessage());
      logger.error("Error in shutDownTaskExecutor : {}", e.getMessage(), e);
      Zoe.getJda().shutdownNow();
      try {
        Zoe.saveDataTxt();
      } catch(FileNotFoundException | UnsupportedEncodingException e1) {
        logger.error("La sauvegarde n'a pas pu être effectué !");
      }
      System.exit(1);
      Thread.currentThread().interrupt();
    }

    logger.info("All ThreadPoolExecutors has safely stop. Now shutdown JDA...");
    channel.sendMessage("All ThreadPoolExecutors has safely stop. Now shutdown discord and save data. (ShutDown is complete)").complete();
    channel.sendMessage("Please wait 30 sec before update Zoe, some process can take some time before to automatically shutdown.").complete();
    
    Zoe.getJda().shutdown();

    while(!Zoe.getJda().getStatus().equals(Status.SHUTDOWN)) {
      try {
        Thread.sleep(100);
      } catch(InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    
    logger.info("JDA has been ShutDown !");

    try {
      Zoe.saveDataTxt();
    } catch(FileNotFoundException | UnsupportedEncodingException e) {
      logger.error("La sauvegarde n'a pas pu être effectué !");
    }
    logger.info("Save has been done ! Zoe Process are now totally down ! Some process can remain and will be shutdown automatically.");
  }
}
