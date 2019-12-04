package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;

public class SelfAddingOption extends ConfigurationOption {

  private boolean optionActivated;

  public SelfAddingOption(long guildId) {
    super(guildId, "selfAddingOptionDesc");
    optionActivated = false;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter, DTO.Server server) {
    return new Consumer<CommandEvent>() {
      
      @Override
      public void accept(CommandEvent event) {
        
        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();
        
        choiceBuilder.setEventWaiter(waiter);
        choiceBuilder.addChoices("✅","❌");
        choiceBuilder.addUsers(event.getAuthor());
        choiceBuilder.setFinalAction(finalAction());
        choiceBuilder.setColor(Color.BLUE);

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);
        
        if(!optionActivated) {

          choiceBuilder.setText(String.format(LanguageManager.getText(server.serv_language, "selfAddingOptionDescLong"), 
              LanguageManager.getText(server.serv_language, description)));
          
          choiceBuilder.setAction(activateTheOption(server.serv_language, event.getChannel()));
          
          ButtonMenu menu = choiceBuilder.build();
                    
          menu.display(event.getChannel());
          
        }else {
          
          choiceBuilder.setText(String.format(LanguageManager.getText(server.serv_language, "selfAddingOptionDescLongDisable"),
              LanguageManager.getText(server.serv_language, description)));
          
          choiceBuilder.setAction(disableTheOption(server.serv_language, event.getChannel()));
          
          ButtonMenu menu = choiceBuilder.build();
          
          menu.display(event.getChannel());
        }
      }
    };
  }
  
  private Consumer<ReactionEmote> disableTheOption(String langage, MessageChannel messageChannel) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emote) {
        
        messageChannel.sendTyping().complete();
        
        if(emote.getName().equals("✅")) {
          optionActivated = false;
          messageChannel.sendMessage(LanguageManager.getText(langage, "selfAddingOptionBeenDisable")).queue();
        }else {
          messageChannel.sendMessage(LanguageManager.getText(langage, "selfAddingOptionStillEnable")).queue();
        }
    }};
  }
  
  private Consumer<ReactionEmote> activateTheOption(String langage, MessageChannel messageChannel) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        
        messageChannel.sendTyping().complete();
        
        if(emoteUsed.getName().equals("✅")) {
          optionActivated = true;
          messageChannel.sendMessage(LanguageManager.getText(langage, "selfAddingOptionBeenActivated")).queue();
        }else {
          messageChannel.sendMessage(LanguageManager.getText(langage, "selfAddingOptionStillDisable")).queue();
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
