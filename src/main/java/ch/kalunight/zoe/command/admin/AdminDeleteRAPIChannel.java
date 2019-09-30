package ch.kalunight.zoe.command.admin;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.service.RiotApiUsageChannelRefresh;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class AdminDeleteRAPIChannel extends Command {

  private static final Logger logger = LoggerFactory.getLogger(AdminDeleteRAPIChannel.class);
  
  public AdminDeleteRAPIChannel() {
    this.name = "deleteRAPIChannel";
    this.help = "Delete the RAPIChannel, even if the channel is in another guild.";
    this.ownerCommand = true;
    this.hidden = true;
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);

    if(RiotApiUsageChannelRefresh.getRapiInfoChannel() == null) {
      event.reply("The RAPIChannel not exist, so i can't delete it.");
      return;
    }

    try {
      RiotApiUsageChannelRefresh.getRapiInfoChannel().delete().complete();
      RiotApiUsageChannelRefresh.setRapiInfoChannel(null);
    }catch(InsufficientPermissionException e) {
      event.reply("I don't have the right to delete the channel");
      return;
    }catch(ErrorResponseException e) {
      if(e.getErrorCode() == ErrorResponse.UNKNOWN_CHANNEL.getCode()) {
        RiotApiUsageChannelRefresh.setRapiInfoChannel(null);
        event.reply("The channel was already deleted. Internal Status updated.");
        return;
      }
      event.reply("Impossible to delete the channel : " + e.getMessage());
      return;
    }
    
    try(PrintWriter writer = new PrintWriter(Zoe.RAPI_SAVE_TXT_FILE, "UTF-8");) {
      writer.write(-1 + "\n" + -1);
    } catch(FileNotFoundException | UnsupportedEncodingException e) {
      event.reply("Error when deleting the channel. This will be have any consequences.");
      logger.warn("Error when deleting rapi InfoChannel : ", e);
    }
    
    event.reply("The channel has been correctly deleted.");
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Admin " + name + " command :\n");
        stringBuilder.append("--> `>admin " + name + " " + arguments + "` : " + help);
        event.reply(stringBuilder.toString());
      }
    };
  }

}
