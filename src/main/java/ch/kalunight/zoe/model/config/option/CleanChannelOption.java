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
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class CleanChannelOption extends ConfigurationOption {

  private static final String UNICODE_ONE = "1\u20E3";
  private static final String EMOJI_ONE = ":one:";

  private static final String UNICODE_TWO = "2\u20E3";
  private static final String EMOJI_TWO = ":two:";

  private static final String UNICODE_THREE = "3\u20E3";
  private static final String EMOJI_THREE = ":three:";

  Logger logger = LoggerFactory.getLogger(CleanChannelOption.class);

  public enum CleanChannelOptionInfo {
    DISABLE("cleanChannelOptionDisable", "cleanChannelOptionDisableDesc", UNICODE_ONE, EMOJI_ONE),
    ONLY_ZOE_COMMANDS("cleanChannelOptionZoeCommands", "cleanChannelOptionZoeCommandsDesc", UNICODE_TWO, EMOJI_TWO),
    ALL("cleanChannelOptionAll", "cleanChannelOptionAllDesc", UNICODE_THREE, EMOJI_THREE);

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
    super("clean_channel", "cleanChannelOptionDesc");   
    cleanChannelOption = CleanChannelOptionInfo.DISABLE;
    cleanChannel = null;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter) {
    return new Consumer<CommandEvent>() {

      @Override
      public void accept(CommandEvent event) {

        Server server = ServerData.getServers().get(event.getGuild().getId());

        if(!event.getGuild().getSelfMember().getPermissions().contains(Permission.MESSAGE_MANAGE)) {
          event.reply(LanguageManager.getText(server.getLangage(), "cleanChannelOptionPermissionNeeded"));
          return;
        }

        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();

        choiceBuilder.setEventWaiter(waiter);        

        choiceBuilder.addChoices(
            CleanChannelOptionInfo.DISABLE.unicode,
            CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.unicode,
            CleanChannelOptionInfo.ALL.unicode,
            "❌");
        choiceBuilder.addUsers(event.getAuthor());
        choiceBuilder.setFinalAction(finalAction());
        choiceBuilder.setColor(Color.BLUE);

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);

        choiceBuilder.setText(
            String.format(LanguageManager.getText(server.getLangage(), "cleanChannelOptionLongDesc"),
                LanguageManager.getText(server.getLangage(), description))
            + CleanChannelOptionInfo.DISABLE.emoji 
            + " -> " + LanguageManager.getText(server.getLangage(), CleanChannelOptionInfo.DISABLE.name)
            + " : " + LanguageManager.getText(server.getLangage(), CleanChannelOptionInfo.DISABLE.description) + "\n"
            + CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.emoji
            + " -> " + LanguageManager.getText(server.getLangage(), CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.name)
            + " : " + LanguageManager.getText(server.getLangage(), CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.description) + "\n"
            + CleanChannelOptionInfo.ALL.emoji
            + " -> " + LanguageManager.getText(server.getLangage(), CleanChannelOptionInfo.ALL.name)
            + " : " + LanguageManager.getText(server.getLangage(), CleanChannelOptionInfo.ALL.description) + "\n");

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
        Server server = ServerData.getServers().get(guild.getId());

        if(emoteUsed.getName().equals(CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.unicode)) {
          tmpCleanChannelOption = CleanChannelOptionInfo.ONLY_ZOE_COMMANDS;
        }else if(emoteUsed.getName().equals(CleanChannelOptionInfo.ALL.unicode)) {
          tmpCleanChannelOption = CleanChannelOptionInfo.ALL;
        }else if(emoteUsed.getName().equals(CleanChannelOptionInfo.DISABLE.unicode)){
          tmpCleanChannelOption = CleanChannelOptionInfo.DISABLE;
        }else {
          channel.sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionCanceled")).queue();
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

          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLangage(), "cleanChannelOptionChoiceChannel"),
              EMOJI_ONE, EMOJI_TWO));

          choiceBuilder.setAction(defineNewChannel(channel, guild, eventWaiter, user));

          ButtonMenu menu = choiceBuilder.build();

          menu.display(channel);
        }else {

          cleanChannel = null;

          if(tmpCleanChannelOption.equals(cleanChannelOption)) {
            channel.sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionAlreadyDisable")).queue();
          }else {
            cleanChannelOption = tmpCleanChannelOption;
            channel.sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionNowDisable")).queue();
          }
        }
      }};
  }


  private Consumer<ReactionEmote> defineNewChannel(MessageChannel channel, Guild guild, EventWaiter eventWaiter, User user) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote reactionEmote) {
        channel.sendTyping().complete();
        Server server = ServerData.getServers().get(guild.getId());
        if(reactionEmote.getName().equals(UNICODE_ONE)) {

          channel.sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionSendTextChannel")).queue();

          eventWaiter.waitForEvent(MessageReceivedEvent.class,
              e -> e.getAuthor().equals(user) && e.getChannel().equals(channel),
              e -> selectChannel(e), 2, TimeUnit.MINUTES,
              () -> endCreateChannelTime(channel));
        }else if(reactionEmote.getName().equals(UNICODE_TWO)) {

          cleanChannel = guild.getTextChannelById(guild.createTextChannel("clean-channel").complete().getId());

          cleanChannelOption = tmpCleanChannelOption;

          if(cleanChannelOption.equals(CleanChannelOptionInfo.ONLY_ZOE_COMMANDS)) {
            cleanChannel.getManager().setTopic(LanguageManager.getText(server.getLangage(),
                "cleanChannelOptionTopicChannelZoeCommands")).queue();
          }else {
            cleanChannel.getManager().setTopic(LanguageManager.getText(server.getLangage(), "cleanChannelOptionTopicChannelAll")).queue();
          }

          channel.sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionCreatedDoneMessage")).queue();

        }
      }
    };
  }

  private void selectChannel(MessageReceivedEvent event) {
    event.getTextChannel().sendTyping().complete();

    Server server = ServerData.getServers().get(event.getGuild().getId());

    List<TextChannel> textsChannel = event.getMessage().getMentionedChannels();

    if(textsChannel.size() == 1) {
      TextChannel textChannel = textsChannel.get(0); 
      if(server.getInfoChannel() == null || !server.getInfoChannel().equals(textChannel)) {

        cleanChannel = textChannel;
        cleanChannelOption = tmpCleanChannelOption;

        if(cleanChannelOption.equals(CleanChannelOptionInfo.ONLY_ZOE_COMMANDS)) {
          textChannel.sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionInfoMessageZoeCommands")).complete();
        }else {
          textChannel.sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionInfoMessageAll")).complete();
        }

        event.getTextChannel().sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionSettedDoneMessage")).queue();

      }else {
        event.getTextChannel().sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionSelectInfoChannel")).queue();
      }
    }else {
      event.getTextChannel().sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionOneTextChannelRequired")).queue();
    }
  }

  private void endCreateChannelTime(MessageChannel channel) {
    TextChannel textChannel = Zoe.getJda().getTextChannelById(channel.getId());

    String langage = LanguageManager.DEFAULT_LANGUAGE;
    if(textChannel != null) {
      Server server = ServerData.getServers().get(textChannel.getGuild().getId());
      if(server != null) {
        langage = server.getLangage();
      }
    }
    channel.sendMessage(LanguageManager.getText(langage, "cleanChannelOptionResponseTimeOut")).queue();
  }

  private Consumer<Message> finalAction(){
    return new Consumer<Message>() {

      @Override
      public void accept(Message message) {
        message.clearReactions().complete();
      }};
  }

  @Override
  public String getChoiceText(String langage) {

    if(cleanChannel != null && Zoe.getJda().getTextChannelById(cleanChannel.getId()) == null) {
      cleanChannel = null;
      cleanChannelOption = CleanChannelOptionInfo.DISABLE;
    }

    String status = LanguageManager.getText(langage, "optionDisable");

    if(cleanChannel != null) {
      status = String.format(LanguageManager.getText(langage, "cleanChannelOptionEnable"),
          LanguageManager.getText(langage, cleanChannelOption.name), cleanChannel.getAsMention());
    }else {
      cleanChannelOption = CleanChannelOptionInfo.DISABLE;
    }

    return LanguageManager.getText(langage, description) + " : " + status;
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
      if(cleanChannel == null) {
        cleanChannelOption = CleanChannelOptionInfo.DISABLE;
      }
    }
  }

  public CleanChannelOptionInfo getCleanChannelOption() {
    return cleanChannelOption;
  }

  public TextChannel getCleanChannel() {
    return cleanChannel;
  }

  public void setCleanChannelOption(CleanChannelOptionInfo cleanChannelOption) {
    this.cleanChannelOption = cleanChannelOption;
  }

  public void setCleanChannel(TextChannel cleanChannel) {
    this.cleanChannel = cleanChannel;
  }

}
