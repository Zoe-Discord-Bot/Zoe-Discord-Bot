package ch.kalunight.zoe.command;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.ConfigurationOption;
import ch.kalunight.zoe.model.config.option.OptionCategory;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.EmojiUtil;
import ch.kalunight.zoe.util.ZoeUserRankManagementUtil;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;

public class ConfigCommandRunnable {

  private static final Logger logger = LoggerFactory.getLogger(ConfigCommandRunnable.class);

  private ConfigCommandRunnable() {
    // hide default constructor
  }

  public static void executeCommand(Server server, EventWaiter waiter, CommandGuildDiscordData event,
      InteractionHook hook) throws SQLException {
    buildCategory(server, waiter, event, hook, new AtomicBoolean(false));
  }

  private static void buildCategory(Server server, EventWaiter waiter, CommandGuildDiscordData event,
      InteractionHook hook, AtomicBoolean needToTimeout) {
    needToTimeout.set(false);

    StringBuilder messageBuilder = new StringBuilder();

    messageBuilder.append(LanguageManager.getText(server.getLanguage(), "configCommandMenuCategoryText"));

    SelectionMenu.Builder categorySelectBuilder = SelectionMenu.create("categorySelect");
    categorySelectBuilder.setPlaceholder(LanguageManager.getText(server.getLanguage(), "configCommandSelectACategory"));

    for(OptionCategory category : OptionCategory.values()) {
      categorySelectBuilder.addOption(LanguageManager.getText(server.getLanguage(), category.getNameId()), category.getId(),
          LanguageManager.getText(server.getLanguage(), category.getDescriptionId()), category.getEmoji());
    }

    Button buttonCancel = Button.secondary(ConfigurationOption.CANCEL_ID, LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelButton"));
    SelectionMenu menuCat = categorySelectBuilder.build();

    ActionRow rowMenu = ActionRow.of(menuCat);
    ActionRow rowButton = ActionRow.of(buttonCancel);

    Message messageToEdit;
    if(hook != null) {
      hook.editOriginal(messageBuilder.toString()).setActionRows(rowMenu, rowButton).complete();
      messageToEdit = hook.retrieveOriginal().complete();
    }else {
      messageToEdit = event.getChannel().sendMessage(messageBuilder.toString()).setActionRows(rowMenu, rowButton).complete();
    }

    final Message endMessage = messageToEdit;

    AtomicBoolean newNeedToTimeout = new AtomicBoolean(true);
    AtomicBoolean reactionDone = new AtomicBoolean(false);
    
    waiter.waitForEvent(SelectionMenuEvent.class,
        e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel()) 
        && e.getMessage().getId().equals(endMessage.getId()) && e.getSelectionMenu().getId().equals("categorySelect"),
        e -> {
          synchronized(reactionDone){
            if(!reactionDone.get()) {
              reactionDone.set(true);
              selectCategory(e, server, waiter, newNeedToTimeout);
            }
          }
        },
        2, TimeUnit.MINUTES,
        () -> cancelOptionCreation(server, event, endMessage, newNeedToTimeout));

    waiter.waitForEvent(ButtonClickEvent.class,
        e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel()) 
        && e.getMessage().getId().equals(endMessage.getId()) && e.getButton().getId().equals(ConfigurationOption.CANCEL_ID),
        e -> {
          synchronized(reactionDone){
            if(!reactionDone.get()) {
              reactionDone.set(true);
              selectButton(e, server, newNeedToTimeout);
            }
          }
        },
        2, TimeUnit.MINUTES,
        () -> cancelOptionCreation(server, event, endMessage, newNeedToTimeout));
  }

  private static void selectButton(ButtonClickEvent e, Server server, AtomicBoolean needToTimeout) {
    needToTimeout.set(false);
    e.deferEdit().queue();
    e.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelUpdate"))
    .setActionRows(new ArrayList<>()).queue();
  }

  private static void selectCategory(SelectionMenuEvent event, Server server, EventWaiter waiter, AtomicBoolean needToTimeout) {
    needToTimeout.set(false);
    event.deferEdit().queue();

    ServerConfiguration config;
    boolean serverBoosted;
    try {
      config = ConfigRepository.getServerConfiguration(server.serv_guildId, event.getJDA());
      serverBoosted = ZoeUserRankManagementUtil.isServerHaveAccessToEarlyAccessFeature(event.getGuild());
    } catch (SQLException e) {
      logger.error("Error while loading server settings from db", e);
      event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).queue();
      return;
    }

    List<ConfigurationOption> options = OptionCategory.getOptionsById(event.getSelectedOptions().get(0).getValue(), config);

    if(options.isEmpty()) {
      logger.warn("Bad id found while searching for option with cat. Bad id : {}", event.getSelectionMenu().getId());
      event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "configCommandMenuErrorNoOptionFoundWithThisCat")).queue();
      return;
    }

    SelectionMenu.Builder optionSelectBuilder = SelectionMenu.create("optionSelect");
    optionSelectBuilder.setPlaceholder(LanguageManager.getText(server.getLanguage(), "configCommandSelectAOption"));

    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(LanguageManager.getText(server.getLanguage(), "configCommandListOption") + "\n\n");

    int emojiCounter = 1;
    for(ConfigurationOption option : options) {

      try {
        String translatedDesc = LanguageManager.getText(server.getLanguage(), option.getDescription());

        String translatedName = LanguageManager.getText(server.getLanguage(), option.getName());

        boolean optionEnable = option.hasAccessToTheFeatures(serverBoosted);

        if(optionEnable) {
          optionSelectBuilder.addOption(translatedName,
              option.getDescription(), translatedDesc, Emoji.fromUnicode(EmojiUtil.getEmojiNumberKeycapString(emojiCounter)));
        }

        messageBuilder.append(EmojiUtil.getEmojiNumberKeycapString(emojiCounter) + " " 
            + option.getChoiceText(server.getLanguage(), serverBoosted) + "\n");
      } catch (SQLException e) {
        logger.error("Error while loading server settings from db", e);
        event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).queue();
        return;
      }
      emojiCounter++;
    }

    messageBuilder.append("\n" + LanguageManager.getText(server.getLanguage(), "configCommandMenuOptionText"));

    Button buttonCancel = Button.secondary("backMenu",
        LanguageManager.getText(server.getLanguage(), "configCommandBackToMenu"));
    ActionRow rowMenu = ActionRow.of(optionSelectBuilder.build());
    ActionRow rowButton = ActionRow.of(buttonCancel);

    event.getHook().editOriginal(messageBuilder.toString()).setActionRows(rowMenu, rowButton).queue();

    AtomicBoolean newNeedToTimeout = new AtomicBoolean(true);
    AtomicBoolean reactionDone = new AtomicBoolean(false);
    
    waiter.waitForEvent(SelectionMenuEvent.class,               
        e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel()) 
        && e.getMessage().equals(event.getMessage()) && e.getSelectionMenu().getId().equals("optionSelect"),
        e -> {
          synchronized(reactionDone){
            if(!reactionDone.get()) {
              reactionDone.set(true);
              selectOption(e, server, waiter, options, newNeedToTimeout);
            }
          }
        },
        2, TimeUnit.MINUTES,
        () -> cancelOptionCreation(server, event, newNeedToTimeout));

    CommandGuildDiscordData commandEvent = new CommandGuildDiscordData(
        event.getMember(), event.getGuild(), event.getTextChannel());

    waiter.waitForEvent(ButtonClickEvent.class,
        e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel())
        && e.getMessage().equals(event.getMessage()) && e.getButton().getId().equals("backMenu"),
        e -> {
          synchronized(reactionDone){
            if(!reactionDone.get()) {
              reactionDone.set(true);
              e.deferEdit().queue();
              buildCategory(server, waiter, commandEvent, event.getHook(), needToTimeout);
            }
          }
        },
        2, TimeUnit.MINUTES,
        () -> cancelOptionCreation(server, event, needToTimeout));
  }

  private static void selectOption(SelectionMenuEvent event, Server server,
      EventWaiter waiter, List<ConfigurationOption> possibleOptions, AtomicBoolean needToTimeout) {
    needToTimeout.set(false);
    event.deferEdit().queue();

    String selectedOptionId = event.getSelectedOptions().get(0).getValue();

    ConfigurationOption option = null;
    for(ConfigurationOption checkForOption : possibleOptions) {
      if(checkForOption.getDescription().equals(selectedOptionId)) {
        option = checkForOption;
        break;
      }
    }

    if(option != null) {
      event.getHook().deleteOriginal().queue();
      option.getChangeConsumer(waiter, server).accept(
          new CommandGuildDiscordData(event.getMember(), event.getGuild(), event.getTextChannel()));
    }else {
      event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(),
          "configCommandMenuOptionErrorNotFound")).setActionRows(new ArrayList<>()).queue();
    }
  }

  private static void cancelOptionCreation(Server server, CommandGuildDiscordData event, Message originalMessage, AtomicBoolean needToTimeout) {
    if(needToTimeout.get()) {
      originalMessage.editMessage(LanguageManager.getText(server.getLanguage(), "configurationEnded")).setActionRows(new ArrayList<>()).queue();
    }
  }

  private static void cancelOptionCreation(Server server, SelectionMenuEvent event, AtomicBoolean needToTimeout) {
    if(needToTimeout.get()) {
      event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "configurationEnded")).setActionRows(new ArrayList<>()).queue();
    }
  }
}
