package ch.kalunight.zoe.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.TextChannel;

public class CommandUtil {
  
  private static final Logger logger = LoggerFactory.getLogger(CommandUtil.class);

  private CommandUtil() {
    //Hide public constructor
  }
  
  public static void sendTypingInFonctionOfChannelType(CommandEvent event) {
    switch(event.getChannelType()) {
    case PRIVATE:
      event.getPrivateChannel().sendTyping().complete();
      break;
    case TEXT:
      event.getTextChannel().sendTyping().complete();
      break;
    default:
      logger.warn("event.getChannelType() return a unexpected type : " + event.getChannelType().toString());
      break;
    }
  }
  
  public static void sendMessageInGuildOrAtOwner(Guild guild, String messageToSend) {
    List<TextChannel> textChannels = guild.getTextChannels();

    boolean messageSended = false;
    for(TextChannel textChannel : textChannels) {
      if(textChannel.canTalk()) {
        textChannel.sendMessage(messageToSend).queue();
        messageSended = true;
        break;
      }
    }

    if(!messageSended) {
      PrivateChannel privateChannel = guild.getOwner().getUser().openPrivateChannel().complete();
      privateChannel.sendMessage(messageToSend).queue();
    }
  }
}
