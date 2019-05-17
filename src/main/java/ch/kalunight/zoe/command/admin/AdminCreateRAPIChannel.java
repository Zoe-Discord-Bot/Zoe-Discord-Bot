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
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;

public class AdminCreateRAPIChannel extends Command {

  private static final Logger logger = LoggerFactory.getLogger(AdminCreateRAPIChannel.class);

  public AdminCreateRAPIChannel() {
    this.name = "createRAPIChannel";
    this.arguments = "NameOfChannel";
    this.help = "Create a new channel where Stats about Riot API Usage is sended, onl";
    this.ownerCommand = true;
    this.hidden = true;
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);

    if(RiotApiUsageChannelRefresh.getRapiInfoChannel() != null) {
      TextChannel textChannel = RiotApiUsageChannelRefresh.getRapiInfoChannel().getGuild()
          .getTextChannelById(RiotApiUsageChannelRefresh.getRapiInfoChannel().getId());
      if(textChannel != null) {
        event.reply("The Riot Api InfoChannel Usage is already defined, please delete it first.");
        return;
      }
    }

    if(event.getArgs().isEmpty()) {
      event.reply("Please send the name of the channel in args (E.g. : `>admin createRAPIChannel RAPIChannel`)");
      return;
    }
    TextChannel rapiTextChannel;
    try {
      Channel rapiChannel = event.getGuild().getController().createTextChannel(event.getArgs()).complete();
      rapiTextChannel = event.getGuild().getTextChannelById(rapiChannel.getId());
    }catch(InsufficientPermissionException e) {
      event.reply("I don't have the right to create a channel in this guild !");
      return;
    }

    try(PrintWriter writer = new PrintWriter(Zoe.RAPI_SAVE_TXT_FILE, "UTF-8");) {
      writer.write(rapiTextChannel.getGuild().getId() + "\n" + rapiTextChannel.getId());
    } catch(FileNotFoundException | UnsupportedEncodingException e) {
      event.reply("Error when saving the channel. Please retry.");
      logger.warn("Error when saving rapi InfoChannel : ", e);
      RiotApiUsageChannelRefresh.setRapiInfoChannel(null);
      rapiTextChannel.delete().queue();
      return;
    }
    RiotApiUsageChannelRefresh.setRapiInfoChannel(rapiTextChannel);

    event.reply("Correctly created, will be refreshed in less than 2 minutes. "
        + "Please don't use this channel, all messages in it will be cleaned every 2 minutes.");
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
