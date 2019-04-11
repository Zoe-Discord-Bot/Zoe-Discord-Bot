package ch.kalunight.zoe.command.admin;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.Server;

public class AdminSendAnnonceMessageCommand extends Command {

  private static final Logger logger = LoggerFactory.getLogger(AdminSendAnnonceMessageCommand.class);
  
  public AdminSendAnnonceMessageCommand() {
    this.name = "sendAnnonce";
    this.arguments = "Text to send";
    this.help = "Send the annonce";
    this.ownerCommand = true;
    this.hidden = true;
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    
    if(event.getArgs().length() >= 2000 || event.getArgs().isEmpty()) {
      event.reply("Message too long or empty ! Max 2000 caracters");
      return;
    }
    
    Iterator<Entry<String, Server>> servers =  ServerData.getServers().entrySet().iterator();
    
    while(servers.hasNext()) {
      Entry<String, Server> server = servers.next();
      
      try {
      CommandUtil.sendMessageInGuildOrAtOwner(server.getValue().getGuild(), event.getArgs());
      }catch(Exception e) {
        logger.warn("Error in sending of the annonce", e);
      }
    }
    
    event.reply("The messsage has been sended !");
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Admin SendUpdateMessage command :\n");
        stringBuilder.append("--> `>admin " + name + " " + arguments + "` : " + help);
        
        event.reply(stringBuilder.toString());
      }
    };
  }
}
