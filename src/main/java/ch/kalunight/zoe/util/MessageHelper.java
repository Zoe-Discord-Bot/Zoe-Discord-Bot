package ch.kalunight.zoe.util;

import java.util.List;
import net.dv8tion.jda.core.entities.TextChannel;

public class MessageHelper {

  private MessageHelper() {}

  public static void sendMessageInOneChannel(List<TextChannel> textChannels, String text) {
    for(TextChannel textChannel : textChannels) {
      if(textChannel.canTalk()) {
        textChannel.sendMessage(text).queue();
        break;
      }
    }
  }
}
