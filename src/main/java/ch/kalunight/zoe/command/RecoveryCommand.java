package ch.kalunight.zoe.command;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreateCommand;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class RecoveryCommand extends Command {

  private static final Logger logger = LoggerFactory.getLogger(RecoveryCommand.class);
  private final EventWaiter waiter;

  public RecoveryCommand(EventWaiter waiter) {
    this.name = "recoverPlayer";
    this.help = "Remake all command of a channel.";
    this.arguments = "@mentionOfSender";
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.waiter = waiter;
    this.cooldown = 120;
  }

  @Override
  protected void execute(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);

    if(event.getMessage().getMentionedMembers().size() == 1 
        && event.getMessage().getMentionedMembers().get(0).getPermissions().contains(Permission.MANAGE_CHANNEL)) {

      event.reply("**WARNING**: This command will relauch all create players commands in this channel (and spam a lot) ! "
          + "Your actual configuration will be drastically modified! "
          + "It's only recommanded to use it if Zoe have forgot your save.\n\n Say **YES** if you want to do that.");

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel()),
          e -> recover(e, event.getMessage().getMentionedMembers().get(0).getUser()), 1,
          TimeUnit.MINUTES,
          () -> cancelRecovery(event.getEvent()));
    }else {
      event.reply("Please mention one people who has the Permission to manage channel");
    }
  }

  private void recover(MessageReceivedEvent e, User user) {
    if(e.getMessage().getContentRaw().equals("YES")) {

      e.getTextChannel().sendMessage("Alright, i start now ...").queue();
      e.getTextChannel().sendMessage("By the power of chocolate moon cake ! Restore all theses lost things !").queueAfter(1, TimeUnit.SECONDS);

      List<Message> messages = e.getTextChannel().getIterableHistory().stream()
          .filter(m-> m.getAuthor().equals(user))
          .limit(1000)
          .collect(Collectors.toList());

      try {
        for(Message potentialCreatePlayer : messages) {
          String message = potentialCreatePlayer.getContentRaw();
          
          if(message.startsWith(Zoe.BOT_PREFIX) && isCreatePlayerCommand(message)) {
            executeCreatePlayerCommand(e.getGuild(), user, potentialCreatePlayer, message);
          }
          
          
          
          
        }
        
        
      }catch(RiotApiException e1) {
        if(e1.getErrorCode() == RiotApiException.SERVER_ERROR) {
          e.getTextChannel().sendMessage("Riot server occured a issue. Please retry after doing a `>reset`.").queue();
          logger.info("Riot api got an error : {}", e1.getMessage(), e1);
        }else if(e1.getErrorCode() == RiotApiException.UNAVAILABLE) {
          e.getTextChannel().sendMessage("Riot server are actually unavailable. Please retry later. (And make a `>reset` before.)").queue();
          logger.info("Riot api is actually unavailable : {}", e1.getMessage(), e1);
        }else {
          e.getTextChannel().sendMessage("I got an unexpected error, Please retry later after doing a `>reset`.").queue();
          logger.warn("Got an unexpected error : {}", e1.getMessage(), e1);
        }
      }
    }else {
      e.getTextChannel().sendMessage("Right, so i do nothing.").queue();
    }
  }

  private void executeCreatePlayerCommand(Guild guild, User user, Message potentialCreatePlayer, String message) throws RiotApiException {
    User playerUser = CreatePlayerCommand.getMentionedUser(potentialCreatePlayer.getMentionedMembers());
    if(playerUser == null) {
      return;
    }

    Server server = ServerData.getServers().get(guild.getId());
    if(CreatePlayerCommand.isTheGivenUserAlreadyRegister(user, server)) {
      return;
    }

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(getCreatePlayerArgsCommand(message));
    if(listArgs.size() != 2) {
      return;
    }

    String regionName = listArgs.get(0);
    String summonerName = listArgs.get(1);

    Platform region = CreatePlayerCommand.getPlatform(regionName);
    if(region == null) {
      return;
    }

    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName, CallPriority.NORMAL);
    }catch(RiotApiException e1) {
      if(e1.getErrorCode() != RiotApiException.DATA_NOT_FOUND) {
        throw e1;
      }
      return;
    }
    
    Player player = new Player(user, summoner, region, false);
    server.getPlayers().add(player);
  }

  private boolean isCreatePlayerCommand(String command) {
    if(command.substring(1).split(" ").length < 4) {
      return false;
    }

    String messageInTreatment = command.substring(1).split(" ")[0];

    return messageInTreatment.equalsIgnoreCase(CreateCommand.USAGE_NAME)
        && command.substring(1).split(" ")[1].equals(CreatePlayerCommand.USAGE_NAME);
  }

  private String getCreatePlayerArgsCommand(String command) {
    return command.substring(1)
        .substring(CreateCommand.USAGE_NAME.length() + 1)
        .substring(CreatePlayerCommand.USAGE_NAME.length() + 1)
        .substring(1);
  }

  private void cancelRecovery(MessageReceivedEvent event) {
    event.getTextChannel().sendMessage("Recovery has been canceled.");
  }


}
