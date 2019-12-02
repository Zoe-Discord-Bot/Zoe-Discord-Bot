package ch.kalunight.zoe.command.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;

public class AdminSendAnnonceMessageCommand extends ZoeCommand {

  private static final Logger logger = LoggerFactory.getLogger(AdminSendAnnonceMessageCommand.class);
  private static final List<String> blackListedSever = new ArrayList<>();

  static {
    blackListedSever.add("264445053596991498"); //Discord Bot List Server
    blackListedSever.add("446425626988249089"); //Bot on Discord
  }
  
  public AdminSendAnnonceMessageCommand() {
    this.name = "sendAnnonce";
    this.arguments = "Text to send";
    this.help = "Send the annonce";
    this.ownerCommand = true;
    this.hidden = true;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildrenNoTranslation(AdminCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) {

    if(event.getArgs().isEmpty()) {
      event.reply("Message empty !");
      return;
    }

    Iterator<Entry<String, Server>> servers = ServerData.getServers().entrySet().iterator();

    List<String> userAlreadySendedId = new ArrayList<>();
    
    while(servers.hasNext()) {
      Entry<String, Server> server = servers.next();

      try {
        Guild actualGuild = server.getValue().getGuild();
        if(!isBlackListed(actualGuild.getId()) && !userAlreadySendedId.contains(actualGuild.getOwnerId())) {
          PrivateChannel privateChannel = actualGuild.getOwner().getUser().openPrivateChannel().complete();
          List<String> messagesToSend = CommandEvent.splitMessage(event.getArgs());
          for(String message : messagesToSend) {
            privateChannel.sendMessage(message).queue();
          }
          userAlreadySendedId.add(actualGuild.getOwnerId());
        }
      } catch(Exception e) {
        logger.warn("Error in sending of the annonce", e);
      }
    }

    event.reply("The messsage has been sended !");
  }
  
  /**
   * Server we know they are busy and not interested by Zoe info Messages (Like discordbot.org server, ect)
   */
  private boolean isBlackListed(String serverId) {
    return blackListedSever.contains(serverId);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
