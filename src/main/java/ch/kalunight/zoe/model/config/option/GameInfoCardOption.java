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

public class GameInfoCardOption extends ConfigurationOption {

  private static final String INFOCARDS_DESC_ID = "infocardsOptionDesc";
  
  private boolean optionActivated;
  
  public GameInfoCardOption(long guildId) {
    super(guildId, INFOCARDS_DESC_ID);
    this.optionActivated = true;
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

          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLanguage(),
              "infocardsOptionLongDescEnable"), LanguageManager.getText(server.getLanguage(), INFOCARDS_DESC_ID)));
          
          choiceBuilder.setAction(activateTheOption(event.getChannel(), server));
          
          ButtonMenu menu = choiceBuilder.build();
                    
          menu.display(event.getChannel());
          
        }else {
          
          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLanguage(), "infocardsOptionLongDescDisable"),
              LanguageManager.getText(server.getLanguage(), INFOCARDS_DESC_ID)));
          
          choiceBuilder.setAction(disableTheOption(event.getChannel(), server));
          
          ButtonMenu menu = choiceBuilder.build();
          
          menu.display(event.getChannel());
        }
      }
        
      };
  }
  
  private Consumer<ReactionEmote> disableTheOption(MessageChannel messageChannel, DTO.Server server) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emote) {
        messageChannel.sendTyping().complete();
        
        if(emote.getName().equals("✅")) {
          try {
            ConfigRepository.updateGameInfoCardOption(guildId, false, messageChannel.getJDA());
            optionActivated = false;
          } catch (SQLException e) {
            RepoRessources.sqlErrorReport(messageChannel, server, e);
            return;
          }
          messageChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionBeenDisable")).queue();
        }else {
          messageChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionStillEnable")).queue();
        }
    }};
  }
  
  private Consumer<ReactionEmote> activateTheOption(MessageChannel messageChannel, DTO.Server server) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        messageChannel.sendTyping().complete();
        
        if(emoteUsed.getName().equals("✅")) {
          try {
            ConfigRepository.updateGameInfoCardOption(guildId, true, messageChannel.getJDA());
            optionActivated = true;
          } catch (SQLException e) {
            RepoRessources.sqlErrorReport(messageChannel, server, e);
            return;
          }
          messageChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionBeenActivated")).queue();
        }else {
          messageChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionStillDisable")).queue();
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
    }else {
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
