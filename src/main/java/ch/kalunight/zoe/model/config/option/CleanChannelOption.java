package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;

import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.repositories.ServerRepository;
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
  
  private static final Logger logger = LoggerFactory.getLogger(CleanChannelOption.class);

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

  public CleanChannelOption(long guildId) {
    super(guildId, "cleanChannelOptionName", "cleanChannelOptionDesc",
        OptionCategory.FEATURES, false);   
    cleanChannelOption = CleanChannelOptionInfo.DISABLE;
    cleanChannel = null;
  }

  @Override
  public Consumer<CommandGuildDiscordData> getChangeConsumer(EventWaiter waiter, DTO.Server server) {
    return new Consumer<CommandGuildDiscordData>() {

      @Override
      public void accept(CommandGuildDiscordData event) {

        if(!event.getGuild().getSelfMember().getPermissions().contains(Permission.MESSAGE_MANAGE)) {
          event.getChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionPermissionNeeded")).queue();
          return;
        }

        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();

        choiceBuilder.setEventWaiter(waiter);        

        choiceBuilder.addChoices(
            CleanChannelOptionInfo.DISABLE.unicode,
            CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.unicode,
            CleanChannelOptionInfo.ALL.unicode,
            "❌");
        choiceBuilder.addUsers(event.getUser());
        choiceBuilder.setFinalAction(finalAction());
        choiceBuilder.setColor(Color.BLUE);

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);

        choiceBuilder.setText(
            String.format(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionLongDesc"),
                LanguageManager.getText(server.getLanguage(), description))
            + "\n" + CleanChannelOptionInfo.DISABLE.emoji 
            + " -> " + LanguageManager.getText(server.getLanguage(), CleanChannelOptionInfo.DISABLE.name)
            + " : " + LanguageManager.getText(server.getLanguage(), CleanChannelOptionInfo.DISABLE.description) + "\n"
            + CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.emoji
            + " -> " + LanguageManager.getText(server.getLanguage(), CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.name)
            + " : " + LanguageManager.getText(server.getLanguage(), CleanChannelOptionInfo.ONLY_ZOE_COMMANDS.description) + "\n"
            + CleanChannelOptionInfo.ALL.emoji
            + " -> " + LanguageManager.getText(server.getLanguage(), CleanChannelOptionInfo.ALL.name)
            + " : " + LanguageManager.getText(server.getLanguage(), CleanChannelOptionInfo.ALL.description) + "\n");

        choiceBuilder.setAction(updateOption(event.getChannel(), event.getGuild(), waiter, event.getUser(), server));

        ButtonMenu menu = choiceBuilder.build();

        menu.display(event.getChannel());
      }};
  }

  private Consumer<ReactionEmote> updateOption(MessageChannel channel, Guild guild, EventWaiter eventWaiter,
      User user, DTO.Server server) {
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
          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionCanceled")).queue();
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

          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionChoiceChannel"),
              EMOJI_ONE, EMOJI_TWO));

          choiceBuilder.setAction(defineNewChannel(channel, guild, eventWaiter, user, server));

          ButtonMenu menu = choiceBuilder.build();

          menu.display(channel);
        }else {

          cleanChannel = null;

          try {
            ConfigRepository.updateCleanChannelOption(guildId, 0, tmpCleanChannelOption.toString(), guild.getJDA());
          } catch(SQLException e) {
            RepoRessources.sqlErrorReport(channel, server, e);
            return;
          }

          if(tmpCleanChannelOption.equals(cleanChannelOption)) {
            channel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionAlreadyDisable")).queue();
          }else {
            cleanChannelOption = tmpCleanChannelOption;
            channel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionNowDisable")).queue();
          }
        }
      }};
  }


  private Consumer<ReactionEmote> defineNewChannel(MessageChannel channel, Guild guild, EventWaiter eventWaiter,
      User user, DTO.Server server) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote reactionEmote) {
        channel.sendTyping().complete();
        if(reactionEmote.getName().equals(UNICODE_ONE)) {

          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionSendTextChannel")).queue();

          eventWaiter.waitForEvent(MessageReceivedEvent.class,
              e -> e.getAuthor().equals(user) && e.getChannel().equals(channel),
              e -> {
                try {
                  selectChannel(e, server);
                } catch(SQLException e1) {
                  RepoRessources.sqlErrorReport(channel, server, e1);
                  return;
                }
              },
              2, TimeUnit.MINUTES,
              () -> endCreateChannelTime(channel));

        }else if(reactionEmote.getName().equals(UNICODE_TWO)) {

          cleanChannel = guild.getTextChannelById(guild.createTextChannel("clean-channel").complete().getId());

          cleanChannelOption = tmpCleanChannelOption;

          try {
            ConfigRepository.updateCleanChannelOption(guildId, cleanChannel.getIdLong(), cleanChannelOption.toString(), guild.getJDA());
          } catch(SQLException e) {
            RepoRessources.sqlErrorReport(cleanChannel, server, e);
            return;
          }

          if(cleanChannelOption.equals(CleanChannelOptionInfo.ONLY_ZOE_COMMANDS)) {
            cleanChannel.getManager().setTopic(LanguageManager.getText(server.getLanguage(),
                "cleanChannelOptionTopicChannelZoeCommands")).queue();
          }else {
            cleanChannel.getManager().setTopic(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionTopicChannelAll")).queue();
          }

          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionCreatedDoneMessage")).queue();

        }
      }
    };
  }

  private void selectChannel(MessageReceivedEvent event, DTO.Server server) throws SQLException {
    event.getTextChannel().sendTyping().complete();

    List<TextChannel> textsChannel = event.getMessage().getMentionedChannels();

    if(textsChannel.size() == 1) {
      TextChannel textChannel = textsChannel.get(0); 
      DTO.InfoChannel infochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);
      if(infochannel == null || infochannel.infochannel_channelid != textChannel.getIdLong()) {

        cleanChannel = textChannel;
        cleanChannelOption = tmpCleanChannelOption;

        ConfigRepository.updateCleanChannelOption(guildId, cleanChannel.getIdLong(), cleanChannelOption.toString(), textChannel.getJDA());

        if(cleanChannelOption.equals(CleanChannelOptionInfo.ONLY_ZOE_COMMANDS)) {
          textChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionInfoMessageZoeCommands")).complete();
        }else {
          textChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionInfoMessageAll")).complete();
        }

        event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionSettedDoneMessage")).queue();

      }else {
        event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionSelectInfoChannel")).queue();
      }
    }else {
      event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionOneTextChannelRequired")).queue();
    }
  }

  private void endCreateChannelTime(MessageChannel channel) {
    TextChannel textChannel = channel.getJDA().getTextChannelById(channel.getId());

    String langage = LanguageManager.DEFAULT_LANGUAGE;
    if(textChannel != null) {
      DTO.Server server = null;
      try {
        server = ServerRepository.getServerWithGuildId(textChannel.getGuild().getIdLong());
      } catch(SQLException e) {
        logger.error("SQL Error when getting the server in CleanChannelOption setup !", e);
      }
      if(server != null) {
        langage = server.getLanguage();
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
  public String getBaseChoiceText(String langage) throws SQLException {

    if(cleanChannel != null && cleanChannel.getJDA().getTextChannelById(cleanChannel.getId()) == null) {
      cleanChannel = null;
      cleanChannelOption = CleanChannelOptionInfo.DISABLE;
      ConfigRepository.updateCleanChannelOption(guildId, 0, cleanChannelOption.toString(), cleanChannel.getJDA());
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
