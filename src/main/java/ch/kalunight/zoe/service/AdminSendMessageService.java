package ch.kalunight.zoe.service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.util.MessageUtil;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;

public class AdminSendMessageService implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(AdminSendMessageService.class);
  private CommandEvent event;

  public AdminSendMessageService(CommandEvent event) {
    this.event = event;
  }

  @Override
  public void run() {
    List<String> userAlreadySendedId = new ArrayList<>();

    for(JDA jdaClient : Zoe.getJDAs()) {
      for(Guild guild : jdaClient.getGuilds()) {

        try {
          if(!Ressources.isBlackListed(guild.getId()) && !userAlreadySendedId.contains(guild.getOwnerId())) {
            PrivateChannel privateChannel = jdaClient.retrieveUserById(guild.getOwnerIdLong()).complete().openPrivateChannel().complete();
            List<String> messagesToSend = MessageUtil.splitMessageToBeSendable(event.getArgs());
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
  }

}
