package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.Server;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class CleanChannelOption extends ConfigurationOption {

  private static final String UNICODE_ONE = "1\u20E3";
  private static final String EMOJI_ONE = ":one:";

  private static final String UNICODE_TWO = "2\u20E3";
  private static final String EMOJI_TWO = ":two:";
  
  private static final String UNICODE_THREE = "3\u20E3";
  private static final String EMOJI_THREE = ":three:";
  
  Logger logger = LoggerFactory.getLogger(CleanChannelOption.class);
  
  public enum CleanChannelOptionInfo {
    DISABLE("Disable", "Option disable", UNICODE_ONE, EMOJI_ONE),
    ONLY_ZOE_COMMANDS("Delete only Zoe commands", "This will delete all new zoe commands in the channel", UNICODE_TWO, EMOJI_TWO),
    ALL("Delete all messages", "This will delete all new messages sended in the channel", UNICODE_THREE, EMOJI_THREE);

    private final String name;
    private final String description;
    private final String unicode;
    private final String emoji;

    private CleanChannelOptionInfo(String name, String description, String unicode, String emoji) {
      this.name = name;
      this.description = description;
      this.unicode = unicode;
      this.emoji = emoji;
    }
  }

  private CleanChannelOptionInfo cleanChannelOption;

  private TextChannel cleanChannel;
  
  /**
   * Used when update option, this is not the state of the option.
   */
  private CleanChannelOptionInfo tmpCleanChannelOption;

  public CleanChannelOption() {
    super("clean_channel", "Create a clean text channel where message/commands are automatically deleted");   
    cleanChannelOption = CleanChannelOptionInfo.DISABLE;
    cleanChannel = null;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter) {
    return new Consumer<CommandEvent>() {

      @Override
      public void accept(CommandEvent event) {

        if(!event.getGuild().getSelfMember().getPermissions().contains(Permission.MESSAGE_MANAGE)) {
          event.reply("I need the message manage permission to activate this option. Please retry after give me this permission.");
          return;
        }

        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();

        choiceBuilder.setEventWaiter(waiter);        
        
        choiceBuilder.addChoices(
            CleanChannelOptionInfo.DISABLE.unicode,
            CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.unicode,
            CleanChannelOptionInfo.ALL.unicode);
        choiceBuilder.addUsers(event.getAuthor());
        choiceBuilder.setFinalAction(finalAction());
        choiceBuilder.setColor(Color.BLUE);

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);

        choiceBuilder.setText("Option : **" + description + "**\n\n"
            + "This option will create a \"clean channel\" where messages are deleted after been sended (with a delay of 3 secs). "
            + "This option can be used in combination with the \"Hide infochannel\" option to make a **registration system** "
            + "for your server (server where you need to register yourself into the bot to acces to the server). "
            + "If a user with the permission Manage channel send a message his message will not be deleted.\n"
            + "**The text channel will be selected after the option selection**.\n\n"
            + "Here are my options : \n"
            + CleanChannelOptionInfo.DISABLE.emoji 
            + " -> " + CleanChannelOptionInfo.DISABLE.name + " : " + CleanChannelOptionInfo.DISABLE.description + "\n"
            + CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.emoji
            + " -> " + CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.name + " : " + CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.description + "\n"
            + CleanChannelOptionInfo.ALL.emoji
            + " -> " + CleanChannelOptionInfo.ALL.name + " : " + CleanChannelOptionInfo.ALL.description + "\n");

        choiceBuilder.setAction(updateOption(event.getChannel(), event.getGuild(), waiter, event.getAuthor()));

        ButtonMenu menu = choiceBuilder.build();

        menu.display(event.getChannel());
      }};
  }
  
  private Consumer<ReactionEmote> updateOption(MessageChannel channel, Guild guild, EventWaiter eventWaiter, User user) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        channel.sendTyping().complete();
        if(emoteUsed.getName().equals(CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.unicode)) {
          tmpCleanChannelOption = CleanChannelOptionInfo.ONLY_ZOE_COMMANDS;
        }else if(emoteUsed.getName().equals(CleanChannelOptionInfo.ALL.unicode)) {
          tmpCleanChannelOption = CleanChannelOptionInfo.ALL;
        }else if(emoteUsed.getName().equals(CleanChannelOptionInfo.DISABLE.unicode)){
          tmpCleanChannelOption = CleanChannelOptionInfo.DISABLE;
        }else {
          logger.warn("Error when receive the emote. Not corresponding.");
          channel.sendMessage("Error in the selection of the emote. Please retry.").queue();
          return;
        }
        
        if(tmpCleanChannelOption == CleanChannelOptionInfo.ONLY_ZOE_COMMANDS 
            || tmpCleanChannelOption == CleanChannelOptionInfo.ALL) {
          
          ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();
          
          choiceBuilder.setEventWaiter(eventWaiter);
          choiceBuilder.addChoices(UNICODE_ONE,UNICODE_TWO);
          choiceBuilder.addUsers(user);
          choiceBuilder.setFinalAction(finalAction());
          choiceBuilder.setColor(Color.BLUE);

          choiceBuilder.setTimeout(2, TimeUnit.MINUTES);
          
          choiceBuilder.setText("Right ! You want to define it in a existant channel or you want to create a new text channel ?\n\n"
              + "Options :\n"
              + EMOJI_ONE + " : Define a exitant channel\n"
              + EMOJI_TWO + " : Create a new channel\n");
          
          choiceBuilder.setAction(defineNewChannel(channel, guild, eventWaiter, user));
          
          ButtonMenu menu = choiceBuilder.build();

          menu.display(channel);
        }else {
          
          cleanChannelOption = tmpCleanChannelOption;
          cleanChannel = null;
          
          channel.sendMessage("Okay ! The option is now disable, i will no longer delete messages in the clean channel.").queue();
          
        }
      }};
  }
  
  
  private Consumer<ReactionEmote> defineNewChannel(MessageChannel channel, Guild guild, EventWaiter eventWaiter, User user){
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote reactionEmote) {
        channel.sendTyping().complete();
        if(reactionEmote.getName().equals(UNICODE_ONE)) {
          
          channel.sendMessage("One more step and we are fine ! Now please send me a message with "
              + "the **Mention** of the text channel you want to define (e.g. #registration-channel). "
              + "*You can't select the InfoChannel.*").queue();
          
          eventWaiter.waitForEvent(MessageReceivedEvent.class,
              e -> e.getAuthor().equals(user) && e.getChannel().equals(channel),
              e -> selectChannel(e), 2, TimeUnit.MINUTES,
              () -> endCreateChannelTime(channel));
        }else if(reactionEmote.getName().equals(UNICODE_TWO)) {
          
          cleanChannel = guild.getTextChannelById(guild.getController().createTextChannel("clean-channel").complete().getId());
          
          cleanChannelOption = tmpCleanChannelOption;
          
          if(cleanChannelOption == CleanChannelOptionInfo.ONLY_ZOE_COMMANDS) {
            cleanChannel.sendMessage("All Zoe command sended here will be deleted after 3 seconds.").queue();
          }else {
            cleanChannel.sendMessage("All messages sended here will be deleted after 3 seconds.").queue();
          }

          channel.sendMessage("Perfect ! I have created a channel with the name \"clean-channel\". "
              + "You can change option of this channel like you want. Just let me my permissions inside "
              + "and everything will be working.").queue();
          
        }
      }
    };
  }
  
  private void selectChannel(MessageReceivedEvent event) {
    event.getTextChannel().sendTyping().complete();
    
    List<TextChannel> textsChannel = event.getMessage().getMentionedChannels();
    
    if(textsChannel.size() != 1) {
     TextChannel textChannel = textsChannel.get(0); 
     Server server = ServerData.getServers().get(textChannel.getId());
     if(!server.getInfoChannel().equals(textChannel)) {
       
       if(cleanChannelOption == CleanChannelOptionInfo.ONLY_ZOE_COMMANDS) {
         textChannel.sendMessage("From now on, Zoe command sended here will be deleted after 3 seconds.").queue();
       }else {
         textChannel.sendMessage("From now on, all messages sended here will be deleted after 3 seconds.").queue();
       }
       
       cleanChannel = textChannel;
       cleanChannelOption = tmpCleanChannelOption;
       
       event.getTextChannel().sendMessage("Perfect ! The channel has been correctly setted. "
           + " Please just check that i have all my needed permissions inside (Manage Messages, Read, Read History).").queue();
       
     }else {
       event.getTextChannel().sendMessage("You have selected the infochannel, this config is impossible to setup. "
           + "The configuration has been canceled.").queue();
     }
    }else {
      event.getTextChannel().sendMessage("You haven't mentionned 1 textchannel. The configuration has been canceled.").queue();
    }
  }
  
  private void endCreateChannelTime(MessageChannel channel) {
    channel.sendMessage("The wait time of 2 minutes has been ended. Please remake the command if you want to setup this option.").queue();
  }
  
  private Consumer<Message> finalAction(){
    return new Consumer<Message>() {

      @Override
      public void accept(Message message) {
        message.clearReactions().complete();
      }};
  }

  @Override
  public String getChoiceText() {
    String status = "";

    if(cleanChannelOption == CleanChannelOptionInfo.DISABLE) {
      status = "Disable";
    } else {
      status = "Enable (" + cleanChannelOption.name + ")";
    }

    return description + " : " + status;
  }

  @Override
  public String getSave() {
    String save = id + ":" + cleanChannelOption.toString();
    if(cleanChannel != null) {
      save += ":" + cleanChannel.getId();
    }

    return save;
  }

  @Override
  public void restoreSave(String save) {
    String[] saveDatas = save.split(":");

    cleanChannelOption = CleanChannelOptionInfo.valueOf(saveDatas[1]);

    if(saveDatas.length > 2) {
      cleanChannel = Zoe.getJda().getTextChannelById(saveDatas[2]);
    }
  }

  public CleanChannelOptionInfo getCleanChannelOption() {
    return cleanChannelOption;
  }
  
  public TextChannel getCleanChannel() {
    return cleanChannel;
  }

}
