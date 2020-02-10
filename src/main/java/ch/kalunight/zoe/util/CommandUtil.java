package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

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

  public static MessageChannel getFullSpeakableChannel(Guild guild) {
    Set<Permission> permissionsNeeded = Collections.synchronizedSet(EnumSet.noneOf(Permission.class));
    permissionsNeeded.add(Permission.MESSAGE_ADD_REACTION);

    Member zoeMember = guild.getMember(Zoe.getJda().getSelfUser());

    for(TextChannel textChannel : guild.getTextChannels()) {
      if(textChannel.canTalk()) {
        Set<Permission> permissions = zoeMember.getPermissions(textChannel);

        if(permissions.containsAll(permissionsNeeded)) {
          return textChannel;
        }
      }
    }
    return null;
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
  
  public static void giveRolePermission(Guild guild, TextChannel channel,
      ServerConfiguration serverConfiguration) {
    Role role = guild.getPublicRole();
    try {
      channel.putPermissionOverride(role).deny(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).queue();
      
      channel.putPermissionOverride(guild.getMember(Zoe.getJda().getSelfUser()))
      .grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).queue();
      
      channel.putPermissionOverride(serverConfiguration.getZoeRoleOption().getRole())
      .grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).queue();
    }catch(InsufficientPermissionException e) {
      logger.info("Missing permission to apply Zoe role option rule, nothing bad");
    }
  }

  public static BiConsumer<CommandEvent, Command> getHelpMethod(String name, String helpId) {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        String language = LanguageManager.DEFAULT_LANGUAGE;
        
        if(event.getChannelType() == ChannelType.TEXT) {
          try {
            language = ServerRepository.getServer(event.getGuild().getIdLong()).serv_language;
          } catch(SQLException e) {
            logger.error("SQL when getting a server in help command!", e);
          }
        }
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name + " " + LanguageManager.getText(language, "command").toLowerCase() + " :\n");
        stringBuilder.append("--> `>" + name + "` : " + LanguageManager.getText(language, helpId));

        event.reply(stringBuilder.toString());
      }
    };
  }

  public static BiConsumer<CommandEvent, Command> getHelpMethodHasChildren(String mainName, Command[] children) {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        
        String language = LanguageManager.DEFAULT_LANGUAGE;
        
        if(event.getChannelType() == ChannelType.TEXT) {
          try {
            language = ServerRepository.getServer(event.getGuild().getIdLong()).serv_language;
          } catch(SQLException e) {
            logger.error("SQL when getting a server in help command!", e);
          }
        }
        
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(mainName + " " + LanguageManager.getText(language, "commandPlural").toLowerCase() + " :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + mainName + " " + commandChildren.getName() + " " + commandChildren.getArguments() + "` : "
              + LanguageManager.getText(language, commandChildren.getHelp()) + "\n");
        }

        event.reply(stringBuilder.toString());
      }
    };
  }

  public static BiConsumer<CommandEvent, Command> getHelpMethodIsChildren(String mainCommandName,
      String commandName, String arguments, String helpId) {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        
        String language = LanguageManager.DEFAULT_LANGUAGE;
        
        if(event.getChannelType() == ChannelType.TEXT) {
          try {
            language = ServerRepository.getServer(event.getGuild().getIdLong()).serv_language;
          } catch(SQLException e) {
            logger.error("SQL when getting a server in help command!", e);
          }
        }
        
        stringBuilder.append(mainCommandName + " " + commandName + " " + LanguageManager.getText(language, "command").toLowerCase() + " :\n");
        if(arguments == null || arguments.equals("")) {
          stringBuilder.append("--> `>" + mainCommandName + " " + commandName + "` : " + helpId);
        }else {
          stringBuilder.append("--> `>" + mainCommandName + " " + commandName + " " + arguments + "` : " + helpId);
        }
        event.reply(stringBuilder.toString());
      }
    };
  }

  public static BiConsumer<CommandEvent, Command> getHelpMethodIsChildrenNoTranslation(String mainCommandName,
      String commandName, String arguments, String help) {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mainCommandName + " " + commandName + " command :\n");
        stringBuilder.append("--> `>" + mainCommandName + " " + commandName + " " + arguments + "` : " + help);
        event.reply(stringBuilder.toString());
      }
    };
  }

  public static BiConsumer<CommandEvent, Command> getHelpMethodHasChildrenNoTranslation(String mainName, Command[] children){
    return new BiConsumer<CommandEvent, Command>() {

      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mainName + " commands :\n");
        for(Command commandChildren : children) {
          stringBuilder.append("--> `>" + mainName + " " + commandChildren.getName() + " " + commandChildren.getArguments() + "` : "
              + commandChildren.getHelp() + "\n");
        }
        event.reply(stringBuilder.toString());
      }
    };
  }

  public static Consumer<CommandEvent> getHelpCommand() {
    return new Consumer<CommandEvent>() {
      @Override
      public void accept(CommandEvent event) {
  
        String language = LanguageManager.DEFAULT_LANGUAGE;
  
        if(event.getChannelType() == ChannelType.TEXT) {
          try {
            language = ServerRepository.getServer(event.getGuild().getIdLong()).serv_language;
          } catch(SQLException e) {
            Zoe.logger.error("Error when doing help command message !", e);
            language = LanguageManager.DEFAULT_LANGUAGE;
          }
          event.reply(LanguageManager.getText(language, "helpMessageSendConfirmation"));
        }
  
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(LanguageManager.getText(language, "startHelpMessage") + "\n\n");
  
        for(Command command : Zoe.getMainCommands(null)) {
  
          if(!command.isHidden() && command.getChildren().length == 0) {
  
            stringBuilder.append(LanguageManager.getText(language, "command") + " **" + command.getName() + "** :\n");
            stringBuilder.append("--> `>" + command.getName() + "` : " + LanguageManager.getText(language, command.getHelp()) + "\n\n");
  
          }else if(!command.isHidden()){
  
            stringBuilder.append(LanguageManager.getText(language, "commandPlural") + " **" + command.getName() + "** : \n");
            for(Command commandChild : command.getChildren()) {
  
              if(commandChild.getArguments() == null || commandChild.getArguments().equals("")) {
                stringBuilder.append("--> `>" + command.getName() + " " + commandChild.getName() + "` : "
                    + LanguageManager.getText(language, commandChild.getHelp()) + "\n");
              }else {
                stringBuilder.append("--> `>" + command.getName() + " " + commandChild.getName() + " " + commandChild.getArguments() + "` : "
                    + LanguageManager.getText(language, commandChild.getHelp()) + "\n");
              }
            }
            stringBuilder.append(" \n");
          }
  
        }
  
        stringBuilder.append(LanguageManager.getText(language, "endHelpMessage") + " https://discord.gg/whc5PrC");
  
        PrivateChannel privateChannel = event.getAuthor().openPrivateChannel().complete();
  
        List<String> helpMessages = CommandEvent.splitMessage(stringBuilder.toString());
  
        for(String helpMessage : helpMessages) {
          privateChannel.sendMessage(helpMessage).queue();
        }
      }
    };
  }
}
