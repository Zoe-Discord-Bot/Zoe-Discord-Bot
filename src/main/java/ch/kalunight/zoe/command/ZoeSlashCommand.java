package ch.kalunight.zoe.command;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.SlashCommand;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public abstract class ZoeSlashCommand extends SlashCommand {

  public static final String USER_OPTION_ID = "user";
  public static final String SUMMONER_OPTION_ID = "summoner-name";
  public static final String REGION_OPTION_ID = "region";
  
  private static final AtomicInteger commandExecuted = new AtomicInteger(0);
  private static final AtomicInteger commandFinishedCorrectly = new AtomicInteger(0);
  private static final AtomicInteger commandFinishedWithError = new AtomicInteger(0);

  protected static final Logger logger = LoggerFactory.getLogger(ZoeSlashCommand.class);
  
  @Override
  protected void execute(SlashCommandEvent event) {
      Runnable commandRunnable = getCommandRunnable(event);
      ServerThreadsManager.getCommandsExecutor().execute(commandRunnable);
  }
  
  private Runnable getCommandRunnable(SlashCommandEvent event) {
    return new Runnable() {

      @Override
      public void run() {
        if(event.getUser().isBot()) {
          logger.debug("The sender is a bot, we ignore his command. ID : {}", event.getUser().getId());
          return;
        }
        event.deferReply().queue();
        logger.info("Command \"{}\" executed", this.getClass().getName());
        commandExecuted.incrementAndGet();

        if(event.getChannelType().equals(ChannelType.TEXT)) {
          try {
            DTO.Server server = ServerRepository.getServerWithGuildId(event.getGuild().getIdLong());
            if(server == null) {
              ServerRepository.createNewServer(event.getGuild().getIdLong(), LanguageManager.DEFAULT_LANGUAGE);
              server = ServerRepository.getServerWithGuildId(event.getGuild().getIdLong());
            }
            ZoeCommand.putServer(server);
          } catch(SQLException e) {
            event.reply("Issue with the db ! Please retry later. Sorry about that :/");
            logger.error("Issue with the db when check if server missing !", e);
            commandFinishedWithError.incrementAndGet();
            return;
          }
        }

        try {
          executeCommand(event);
        } catch (InsufficientPermissionException e) {
          logger.info("Unexpected exception in {} commands. Error : {}", this.getClass().getName(), e.getMessage(), e);
          event.reply(String.format(LanguageManager.getText(ZoeCommand.getServer(event.getGuild().getIdLong()).getLanguage(), "deletePlayerMissingPermission"), 
              e.getPermission().getName()));
          commandFinishedCorrectly.incrementAndGet();
          return;
        } catch (Exception e) {
          logger.error("Unexpected exception in {} commands. Error : {}", this.getClass().getName(), e.getMessage(), e);
          event.reply("An error as occured ! It has been saved and sended to the dev. Sorry About that :/");
          commandFinishedWithError.incrementAndGet();
          return;
        }
        logger.info("Command \"{}\" finished correctly", this.getClass().getName());
        commandFinishedCorrectly.incrementAndGet();
      }
    };
  }
  
  protected abstract void executeCommand(SlashCommandEvent event) throws SQLException;
  
  public static void clearStats() {
    commandExecuted.set(0);
    commandFinishedCorrectly.set(0);
    commandFinishedWithError.set(0);
  }
  
  public static AtomicInteger getCommandExecuted() {
    return commandExecuted;
  }

  public static AtomicInteger getCommandFinishedCorrectly() {
    return commandFinishedCorrectly;
  }

  public static AtomicInteger getCommandFinishedWithError() {
    return commandFinishedWithError;
  }
}
