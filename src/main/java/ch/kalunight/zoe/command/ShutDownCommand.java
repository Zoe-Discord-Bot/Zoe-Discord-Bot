package ch.kalunight.zoe.command;

import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.repositories.RepoRessources;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.entities.TextChannel;

public class ShutDownCommand extends ZoeCommand {

  private static final Logger logger = LoggerFactory.getLogger(ShutDownCommand.class);

  public ShutDownCommand() {
    this.name = "stop";
    this.help = "Safely shutdown the bot";
    this.hidden = true;
    this.ownerCommand = true;
    this.guildOnly = false;
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    TextChannel channel = event.getTextChannel();

    if(channel == null) {
      event.reply("Please send this command in a guild text channel !");
      return;
    }
    
    logger.info("Shutdown started !");
    
    channel.sendMessage("ShutDown Started ! I will not respond anymore ...").complete();

    for(Object eventListener : Zoe.getEventlistenerlist()) {
      Zoe.getJda().removeEventListener(eventListener);
    }
    
    Zoe.getJda().getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.of(ActivityType.DEFAULT, "Shuting down ..."));
    
    ServerThreadsManager.getServerCheckerThreadTimer().cancel();
    ServerThreadsManager.getDiscordDetectionDelayedTask().cancel();

    logger.info("The server checker thread has been safely stopped ...");
    channel.sendMessage("The server checker thread has been safely stopped ...").complete();

    try {
      ServerThreadsManager.shutDownTaskExecutor(channel);
    } catch(InterruptedException e) {
      event.reply("Thread got interupted ! Shut down without finish task : " + e.getMessage());
      logger.error("Error in shutDownTaskExecutor : {}", e.getMessage(), e);
      Zoe.getJda().shutdownNow();
      System.exit(1);
      Thread.currentThread().interrupt();
    }

    logger.info("All ThreadPoolExecutors has safely stop. Now shutdown JDA...");
    channel.sendMessage("All ThreadPoolExecutors has safely stop. Now shutdown JDA. (ShutDown is complete)").complete();
    channel.sendMessage("Please wait 30 sec before update Zoe, some process can take some time before to automatically shutdown.").complete();
    
    Zoe.getJda().shutdown();

    while(!Zoe.getJda().getStatus().equals(Status.SHUTDOWN)) {
      try {
        Thread.sleep(100);
      } catch(InterruptedException e) {
        logger.error("Thread got interuped !");
        Thread.currentThread().interrupt();
      }
    }
    
    RepoRessources.shutdownDB();
    
    logger.info("JDA has been ShutDown !");
    logger.info("Zoe Process are now totally down ! Some process can remain and will be shutdown automatically.");
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
