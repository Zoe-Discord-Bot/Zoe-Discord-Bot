package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;

import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;

public class SelfAddingOption extends ConfigurationOption {

  private boolean optionActivated;

  public SelfAddingOption(long guildId) {
    super(guildId, "selfAddingOptionName", "selfAddingOptionDesc", OptionCategory.USER_MANAGEMENT);
    optionActivated = false;
  }

  @Override
  public Consumer<CommandGuildDiscordData> getChangeConsumer(EventWaiter waiter, DTO.Server server) {
    return new Consumer<CommandGuildDiscordData>() {
      
      @Override
      public void accept(CommandGuildDiscordData event) {
        
        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();
        
        choiceBuilder.setEventWaiter(waiter);
        choiceBuilder.addChoices("✅","❌");
        choiceBuilder.addUsers(event.getUser());
        choiceBuilder.setFinalAction(finalAction());
        choiceBuilder.setColor(Color.BLUE);

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);
        
        if(!optionActivated) {

          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLanguage(), "selfAddingOptionDescLong"), 
              LanguageManager.getText(server.getLanguage(), description)));
          
          choiceBuilder.setAction(activateTheOption(server, event.getChannel()));
          
          ButtonMenu menu = choiceBuilder.build();
                    
          menu.display(event.getChannel());
          
        }else {
          
          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLanguage(), "selfAddingOptionDescLongDisable"),
              LanguageManager.getText(server.getLanguage(), description)));
          
          choiceBuilder.setAction(disableTheOption(server, event.getChannel()));
          
          ButtonMenu menu = choiceBuilder.build();
          
          menu.display(event.getChannel());
        }
      }
    };
  }
  
  private Consumer<ReactionEmote> disableTheOption(DTO.Server server, MessageChannel messageChannel) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emote) {
        
        messageChannel.sendTyping().complete();
        
        if(emote.getName().equals("✅")) {
          optionActivated = false;
          try {
            ConfigRepository.updateSelfAddingOption(guildId, optionActivated, messageChannel.getJDA());
          } catch (SQLException e) {
            RepoRessources.sqlErrorReport(messageChannel, server, e);
            return;
          }
          messageChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "selfAddingOptionBeenDisable")).queue();
        }else {
          messageChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "selfAddingOptionStillEnable")).queue();
        }
    }};
  }
  
  private Consumer<ReactionEmote> activateTheOption(DTO.Server server, MessageChannel messageChannel) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        
        messageChannel.sendTyping().complete();
        
        if(emoteUsed.getName().equals("✅")) {
          optionActivated = true;
          try {
            ConfigRepository.updateSelfAddingOption(guildId, optionActivated, messageChannel.getJDA());
          } catch (SQLException e) {
            RepoRessources.sqlErrorReport(messageChannel, server, e);
            return;
          }
          
          messageChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "selfAddingOptionBeenActivated")).queue();
        }else {
          messageChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "selfAddingOptionStillDisable")).queue();
        }
      }};
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
    String status;
    
    if(optionActivated) {
      status = LanguageManager.getText(langage, "optionEnable");
    } else {
      status = LanguageManager.getText(langage, "optionDisable");
    }
    return LanguageManager.getText(langage, description) + " : " + status;
  }

  public boolean isOptionActivated() {
    return optionActivated;
  }

  public void setOptionActivated(boolean optionActivated) {
    this.optionActivated = optionActivated;
  }

}
