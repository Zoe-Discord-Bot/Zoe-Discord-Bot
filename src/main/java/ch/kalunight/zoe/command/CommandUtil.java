package ch.kalunight.zoe.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.CommandEvent;

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
}
