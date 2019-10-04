package ch.kalunight.zoe.command;

import java.util.List;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class CommandUtil {

  private static final Logger logger = LoggerFactory.getLogger(CommandUtil.class);

  private CommandUtil() {
    // Hide public constructor
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
        logger.warn("event.getChannelType() return a unexpected type : {}", event.getChannelType());
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

    try {
      if(!messageSended) {
        PrivateChannel privateChannel = guild.getOwner().getUser().openPrivateChannel().complete();
        privateChannel.sendMessage(messageToSend).queue();
      }
    } catch(ErrorResponseException e) {
      logger.info("Impossible to send the message to the a owner.");
    }
  }
  
  public static BiConsumer<CommandEvent, Command> getHelpMethod(String name, String helpId) {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        Server server = ServerData.getServers().get(event.getGuild().getId());
        
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name + " command :\n");
        stringBuilder.append("--> `>" + name + " " + "` : " + LanguageManager.getText(server.getLangage(), helpId));

        event.reply(stringBuilder.toString());
      }
    };
  }
}
