package ch.kalunight.zoe.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.add.AddCommand;
import ch.kalunight.zoe.command.add.AddPlayerToTeam;
import ch.kalunight.zoe.command.create.CreateCommand;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.command.create.CreateTeamCommand;
import ch.kalunight.zoe.command.delete.DeleteCommand;
import ch.kalunight.zoe.command.delete.DeletePlayerCommand;
import ch.kalunight.zoe.command.delete.DeleteTeamCommand;
import ch.kalunight.zoe.command.remove.RemoveCommand;
import ch.kalunight.zoe.command.remove.RemovePlayerToTeam;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.Team;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
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
    this.name = "recover";
    this.help = "Remake all config command of a channel from mentioned user.";
    this.arguments = "@mentionOfConfigurator (You can mention multiple people)";
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.waiter = waiter;
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);

    if(!event.getMessage().getMentionedMembers().isEmpty() 
        && event.getMessage().getMentionedMembers().get(0).getPermissions().contains(Permission.MANAGE_CHANNEL)) {

      event.getEvent().getTextChannel().sendMessage("**WARNING**: This command will relauch all configuration commands existing in this channel ! "
          + "Your actual configuration will be drastically modified! This command will take a long time to be execute. "
          + "It's only recommanded to use it if Zoe have forgot your save.\n\nSay **YES** if you want to do that.").complete();

      List<String> users = new ArrayList<>();

      for(Member member : event.getMessage().getMentionedMembers()) {
        users.add(member.getUser().getId());
      }

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
            && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> recover(e, users), 1, TimeUnit.MINUTES,
          () -> cancelRecovery(event.getEvent()));
    }else {
      event.reply("Please mention at least one people who has the Permission to manage channel");
    }
  }

  private void recover(MessageReceivedEvent messageReceivedEvent, List<String> usersId) {
    if(messageReceivedEvent.getMessage().getContentRaw().equals("YES")) {

      messageReceivedEvent.getTextChannel().sendMessage("Alright, i start now ...").queue();
      messageReceivedEvent.getTextChannel().sendMessage("By the power of chocolate moon cake ! Restore all theses lost things !").queueAfter(1, TimeUnit.SECONDS);

      List<Message> reverseMessages = messageReceivedEvent.getTextChannel().getIterableHistory().stream()
          .filter(m-> usersId.contains(m.getAuthor().getId()) && m.getContentRaw().startsWith(Zoe.BOT_PREFIX)) //NEED TO REVERT THIS STREAM
          .limit(1000)
          .collect(Collectors.toList());
      
      List<Message> messages = Lists.reverse(reverseMessages);

      try {
        Server server = ServerData.getServers().get(messageReceivedEvent.getGuild().getId());
        for(Message potentialCommand : messages) {
          String message = potentialCommand.getContentRaw();

            if(isCreatePlayerCommand(message)) {
              executeCreatePlayerCommand(potentialCommand, server);
            }

            if(isDeletePlayerCommand(message)) {
              executeDeletePlayerCommand(potentialCommand, server);
            }

            if(isCreateTeamCommand(message)) {
              executeCreateTeamCommand(potentialCommand, server);
            }

            if(isDeleteTeamCommand(message)) {
              executeDeleteTeamCommand(potentialCommand, server);
            }

            if(isAddPlayerToTeamCommand(message)) {
              executeAddPlayerToTeamCommand(potentialCommand, server);
            }
            
            if(isRemovePlayerToTeamCommand(message)) {
              executeRemovePlayerToTeamCommand(potentialCommand, server);
            }
        }
        
        messageReceivedEvent.getTextChannel().sendMessage("Recovery finished !").queue();
        
      }catch(RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.SERVER_ERROR) {
          logger.info("Riot api got an error : {}", e.getMessage(), e);
          messageReceivedEvent.getTextChannel().sendMessage("Riot server occured a issue. Please retry after doing a `>reset`.").queue();
        }else if(e.getErrorCode() == RiotApiException.UNAVAILABLE) {
          logger.info("Riot api is actually unavailable : {}", e.getMessage(), e);
          messageReceivedEvent.getTextChannel().sendMessage("Riot server are actually unavailable. Please retry later. (And make a `>reset` before.)").queue();
        }else {
          logger.error("Got an unexpected error : {}", e.getMessage(), e);
          messageReceivedEvent.getTextChannel().sendMessage("I got an unexpected error, Please retry later after doing a `>reset`."
              + " If it doesn't resolve your issue ask for help to the support server.").queue();
        }
      } catch(Exception e) {
        logger.error("Unexpected error : {}", e.getMessage(), e);
        messageReceivedEvent.getTextChannel().sendMessage("I got a unexpected issue, please retry. "
            + "If the error remain, please contact the support server.").queue();
      }
    }else {
      messageReceivedEvent.getTextChannel().sendMessage("Right, so i do nothing.").queue();
    }
  }

  private void executeRemovePlayerToTeamCommand(Message potentialCommand, Server server) {
    
    if(potentialCommand.getMentionedMembers().size() != 1) {
      return;
    }
    
    Player player = server.getPlayerByDiscordId(potentialCommand.getMentionedMembers().get(0).getUser().getId());

    if(player == null) {
      return;
    }
    
    Matcher matcher = RemovePlayerToTeam.PARENTHESES_PATTERN.matcher(
        getArgsCommand(potentialCommand.getContentRaw(), RemoveCommand.USAGE_NAME, RemovePlayerToTeam.USAGE_NAME));
    String teamName = "";
    while(matcher.find()) {
      teamName = matcher.group(1);
    }

    Team teamWhereRemove = server.getTeamByName(teamName);
    if(teamWhereRemove == null) {
      return;
    }

    if(!teamWhereRemove.isPlayerInTheTeam(player)) {
      return;
    }

    teamWhereRemove.getPlayers().remove(player);
  }

  private void executeAddPlayerToTeamCommand(Message potentialCommand, Server server) {
    
    if(potentialCommand.getMentionedMembers().size() == 1) {
      Player player = server.getPlayerByDiscordId(potentialCommand.getMentionedMembers().get(0).getUser().getId());
      
      if(player != null) {
        Team team = server.getTeamByPlayer(player);
        
        if(team == null) {
          Matcher matcher = AddPlayerToTeam.PARENTHESES_PATTERN.matcher(
              getArgsCommand(potentialCommand.getContentRaw(), AddCommand.USAGE_NAME, AddPlayerToTeam.USAGE_NAME));
          String teamName = "";
          while(matcher.find()) {
            teamName = matcher.group(1);
          }

          Team teamToAdd = server.getTeamByName(teamName);
          
          if(teamToAdd != null) {
            teamToAdd.getPlayers().add(player);
          }
        }
      }
    }
  }

  private void executeDeleteTeamCommand(Message potentialCommand, Server server) {
    String teamName = getArgsCommand(potentialCommand.getContentRaw(), DeleteCommand.USAGE_NAME, DeleteTeamCommand.USAGE_NAME);

    Team team = server.getTeamByName(teamName);
    if(team != null) {
      server.getTeams().remove(team);
    }
  }

  private void executeCreateTeamCommand(Message message, Server server) {

    String nameTeam = getArgsCommand(message.getContentRaw(), CreateCommand.USAGE_NAME, CreateTeamCommand.USAGE_NAME);

    if(!nameTeam.equals("")) {
      Team team = server.getTeamByName(nameTeam);

      if(team == null) {
        server.getTeams().add(new Team(nameTeam));
      }
    }
  }

  private void executeDeletePlayerCommand(Message potentialCreatePlayer, Server server) {
    List<Member> members = potentialCreatePlayer.getMentionedMembers();

    if(members.size() == 1) {
      User userToDelete = members.get(0).getUser();
      Player player = server.getPlayerByDiscordId(userToDelete.getId());

      if(player != null) {
        server.deletePlayer(player);
      }
    }
  }

  private void executeCreatePlayerCommand(Message potentialCreatePlayer, Server server) throws RiotApiException {
    User playerUser = CreatePlayerCommand.getMentionedUser(potentialCreatePlayer.getMentionedMembers());
    if(playerUser == null) {
      return;
    }

    if(CreatePlayerCommand.isTheGivenUserAlreadyRegister(potentialCreatePlayer.getAuthor(), server)) {
      return;
    }

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(
        getArgsCommand(potentialCreatePlayer.getContentRaw(), CreateCommand.USAGE_NAME, CreatePlayerCommand.USAGE_NAME));
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

    Player player = new Player(potentialCreatePlayer.getAuthor(), summoner, region, false);
    server.getPlayers().add(player);
  }

  private boolean isAddPlayerToTeamCommand(String command) {
    if(command.split(" ").length < 4) { // Minimum 4 bloc of text
      return false;
    }
    
    String messageInTreatment = command.substring(Zoe.BOT_PREFIX.length()).split(" ")[0];

    return messageInTreatment.equalsIgnoreCase(AddCommand.USAGE_NAME)
        && command.substring(Zoe.BOT_PREFIX.length()).split(" ")[1].equalsIgnoreCase(AddPlayerToTeam.USAGE_NAME);
  }
  
  private boolean isRemovePlayerToTeamCommand(String command) {
    if(command.split(" ").length < 4) { // Minimum 4 bloc of text
      return false;
    }
    String messageInTreatment = command.substring(Zoe.BOT_PREFIX.length()).split(" ")[0];

    return messageInTreatment.equalsIgnoreCase(RemoveCommand.USAGE_NAME)
        && command.substring(Zoe.BOT_PREFIX.length()).split(" ")[1].equalsIgnoreCase(RemovePlayerToTeam.USAGE_NAME);
  }
  
  private boolean isCreatePlayerCommand(String command) {
    if(command.split(" ").length < 4) { // Minimum 4 bloc of text
      return false;
    }

    String messageInTreatment = command.substring(Zoe.BOT_PREFIX.length()).split(" ")[0];

    return messageInTreatment.equalsIgnoreCase(CreateCommand.USAGE_NAME)
        && command.substring(Zoe.BOT_PREFIX.length()).split(" ")[1].equals(CreatePlayerCommand.USAGE_NAME);
  }

  private boolean isCreateTeamCommand(String command) {
    if(command.split(" ").length < 4) { // Minimum 4 bloc of text
      return false;
    }

    String messageInTreatment = command.substring(Zoe.BOT_PREFIX.length()).split(" ")[0];

    return messageInTreatment.equalsIgnoreCase(CreateCommand.USAGE_NAME)
        && command.substring(Zoe.BOT_PREFIX.length()).split(" ")[1].equals(CreateTeamCommand.USAGE_NAME);
  }

  private boolean isDeleteTeamCommand(String command) {
    if(command.split(" ").length < 3) { // Minimum 3 block of text
      return false;
    }

    String messageInTreatment = command.substring(Zoe.BOT_PREFIX.length()).split(" ")[0];

    return messageInTreatment.equalsIgnoreCase(DeleteCommand.USAGE_NAME)
        && command.substring(Zoe.BOT_PREFIX.length()).split(" ")[1].equals(DeleteTeamCommand.USAGE_NAME);
  }

  private boolean isDeletePlayerCommand(String command) {
    if(command.split(" ").length < 3) { // Minimum 3 bloc of text
      return false;
    }

    String messageInTreatment = command.substring(1).split(" ")[0];

    return messageInTreatment.equalsIgnoreCase(DeleteCommand.USAGE_NAME)
        && command.substring(Zoe.BOT_PREFIX.length()).split(" ")[1].equals(DeletePlayerCommand.USAGE_NAME);
  }

  private String getArgsCommand(String command, String mainName, String usage) {
    return command.substring(Zoe.BOT_PREFIX.length())
        .substring(mainName.length() + 1)
        .substring(usage.length() + 1)
        .substring(1);
  }

  private void cancelRecovery(MessageReceivedEvent event) {
    event.getTextChannel().sendMessage("Recovery has been canceled.");
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Recovery command :\n");
        stringBuilder.append("--> `>" + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
}
