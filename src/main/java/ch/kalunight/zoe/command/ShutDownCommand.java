package ch.kalunight.zoe.command;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.Server;
import net.dv8tion.jda.core.JDA.Status;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

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
    
    channel.sendTyping().complete();
    
    channel.sendMessage("I will be shutdown ...").complete();
    
    ServerData.getMainThreadTimer().cancel();
    
    channel.sendMessage("The game checker thread has been safely stopped ...").complete();
    
    try {
      ServerData.shutDownTaskExecutor();
    } catch (InterruptedException e) {
      event.reply("Task executor got a error : " + e.getMessage());
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
    
    channel.sendMessage("Task executor has safely stop ...").complete();
    
    Set<Entry<String, Server>> serversEntry = ServerData.getServers().entrySet();
    
    for(Entry<String, Server> serverEntry : serversEntry) {
      Server server = serverEntry.getValue();
      
      if(server != null) {
        List<InfoCard> infoCards = server.getControlePannel().getInfoCards();
        ArrayList<Message> messageToDelete = new ArrayList<>();
        for(InfoCard infoCard : infoCards) {
          messageToDelete.add(infoCard.getMessage());
          messageToDelete.add(infoCard.getTitle());
        }
        server.getInfoChannel().deleteMessages(messageToDelete).complete();
      }
    }
    
    channel.sendMessage("All info cards has been deleted, now shutdown discord and save data. (ShutDown is complete)").complete();
    
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
