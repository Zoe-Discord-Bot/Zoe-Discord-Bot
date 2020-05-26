package ch.kalunight.zoe.command.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;

public class AdminSendAnnonceMessageCommand extends ZoeCommand {

  private static final Logger logger = LoggerFactory.getLogger(AdminSendAnnonceMessageCommand.class);
  
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

    List<String> userAlreadySendedId = new ArrayList<>();
    
    for(Guild guild : Zoe.getJda().getGuilds()) {

      try {
        if(!Ressources.isBlackListed(guild.getId()) && !userAlreadySendedId.contains(guild.getOwnerId())) {
          PrivateChannel privateChannel = guild.getOwner().getUser().openPrivateChannel().complete();
          List<String> messagesToSend = CommandEvent.splitMessage(event.getArgs());
          for(String message : messagesToSend) {
            privateChannel.sendMessage(message).queue();
          }
          userAlreadySendedId.add(guild.getOwnerId());
        }
      } catch(Exception e) {
        logger.warn("Error in sending of the annonce", e);
      }
    }

    event.reply("The messsage has been sended !");
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
