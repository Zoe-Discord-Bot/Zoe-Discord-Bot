package ch.kalunight.zoe.command;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ZoeMain;
import net.dv8tion.jda.core.JDA.Status;

public class ShutDownCommand extends Command {

  private static final Logger logger = LoggerFactory.getLogger(ShutDownCommand.class);

  public ShutDownCommand() {
    this.name = "stop";
    this.help = "Safely shutdown the bot";
    this.hidden = true;
    this.ownerCommand = true;
  }

  @Override
  protected void execute(CommandEvent event) {

    ZoeMain.getJda().shutdown();
    
    while(!ZoeMain.getJda().getStatus().equals(Status.SHUTDOWN));
    
    try {
      ZoeMain.saveDataTxt();
    } catch(FileNotFoundException | UnsupportedEncodingException e) {
      logger.error("La sauvegarde n'a pas pu être effectué !");
    }
    
  }
}
