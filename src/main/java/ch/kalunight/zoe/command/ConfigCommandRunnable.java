package ch.kalunight.zoe.command;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.EventListener;
import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.ConfigurationOption;
import ch.kalunight.zoe.model.config.option.OptionCategory;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.EmojiUtil;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;

public class ConfigCommandRunnable {
  
  private static final Logger logger = LoggerFactory.getLogger(ConfigCommandRunnable.class);
  
  private ConfigCommandRunnable() {
    // hide default constructor
  }
  
  public static void executeCommand(Server server, EventWaiter waiter, CommandGuildDiscordData event) throws SQLException {
    
    StringBuilder messageBuilder = new StringBuilder();
    
    messageBuilder.append(LanguageManager.getText(server.getLanguage(), "configCommandMenuCategoryText"));
    
    SelectionMenu.Builder categorySelectBuilder = SelectionMenu.create("categorySelect");
    categorySelectBuilder.setPlaceholder(LanguageManager.getText(server.getLanguage(), "configCommandSelectACategory"));
    
    for(OptionCategory category : OptionCategory.values()) {
      categorySelectBuilder.addOption(LanguageManager.getText(server.getLanguage(), category.getNameId()), category.getId(),
          LanguageManager.getText(server.getLanguage(), category.getDescriptionId()), category.getEmoji());
    }
    
    SelectionMenu menuCat = categorySelectBuilder.build();
    Message message = event.getChannel().sendMessage(messageBuilder.toString()).setActionRow(menuCat).setActionRow(menuCat).complete();
    
    waiter.waitForEvent(SelectionMenuEvent.class,               
        e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel()) && e.getMessage().equals(message),
        e -> {
          selectCategory(e, server, waiter);
        },
        2, TimeUnit.MINUTES,
        () -> cancelOptionCreation(server, event, message));
  }
  
  private static void selectCategory(SelectionMenuEvent event, Server server, EventWaiter waiter) {
    event.deferEdit().queue();
    
    ServerConfiguration config;
    try {
      config = ConfigRepository.getServerConfiguration(server.serv_guildId, event.getJDA());
    } catch (SQLException e) {
      logger.error("Error while loading server settings from db", e);
      event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).queue();
      return;
    }
    
    List<ConfigurationOption> options = OptionCategory.getOptionsById(event.getSelectionMenu().getId(), config);
    
    if(options.isEmpty()) {
      logger.warn("Bad id found while searching for option with cat. Bad id : {}", event.getSelectionMenu().getId());
      event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "configCommandMenuErrorNoOptionFoundWithThisCat")).queue();
      return;
    }
    
    SelectionMenu.Builder optionSelectBuilder = SelectionMenu.create("optionSelect");
    optionSelectBuilder.setPlaceholder(LanguageManager.getText(server.getLanguage(), "configCommandSelectAOption"));
    
    int emojiCounter = 1;
    for(ConfigurationOption option : options) {
      try {
        optionSelectBuilder.addOption(LanguageManager.getText(server.getLanguage(), option.getDescription()),
            option.getDescription(), option.getChoiceText(server.getLanguage()), Emoji.fromUnicode(EmojiUtil.getEmojiNumberKeycapString(emojiCounter)));
      } catch (SQLException e) {
        logger.error("Error while loading server settings from db", e);
        event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).queue();
        return;
      }
      emojiCounter++;
    }
    
    event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "configCommandMenuOptionText")).setActionRow(optionSelectBuilder.build()).queue();
    
    waiter.waitForEvent(SelectionMenuEvent.class,               
        e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel()) 
        && e.getMessage().equals(event.getMessage()),
        e -> {
          selectOption(e, server, waiter);
        },
        2, TimeUnit.MINUTES,
        () -> cancelOptionCreation(server, event));
  }

  private static void selectOption(SelectionMenuEvent event, Server server, EventWaiter waiter) {
	  event.deferEdit().queue();
	  
	  
  }

  private static void cancelOptionCreation(Server server, CommandGuildDiscordData event, Message originalMessage) {
    originalMessage.editMessage(LanguageManager.getText(server.getLanguage(), "configurationEnded")).setActionRows(new ArrayList<>()).queue();
  }
  
  private static void cancelOptionCreation(Server server, SelectionMenuEvent event) {
    event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "configurationEnded")).setActionRows(new ArrayList<>()).queue();
  }
}
