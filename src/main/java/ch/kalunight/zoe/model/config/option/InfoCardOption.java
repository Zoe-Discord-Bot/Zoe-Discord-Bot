package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.static_data.SpellingLanguage;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;

public class InfoCardOption extends ConfigurationOption {

  private static final String INFOCARDS_DESC_ID = "infocardsOptionDesc";
  
  private boolean optionActivated;
  
  public InfoCardOption() {
    super("infocards", INFOCARDS_DESC_ID);
    this.optionActivated = true;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter) {
    return new Consumer<CommandEvent>() {
      
      @Override
      public void accept(CommandEvent event) {
        
        Server server = ServerData.getServers().get(event.getGuild().getId());
        
        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();
        
        choiceBuilder.setEventWaiter(waiter);
        choiceBuilder.addChoices("✅","❌");
        choiceBuilder.addUsers(event.getAuthor());
        choiceBuilder.setFinalAction(finalAction());
        choiceBuilder.setColor(Color.BLUE);

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);
        
        if(!optionActivated) {

          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLangage(),
              "infocardsOptionLongDescEnable"), LanguageManager.getText(server.getLangage(), INFOCARDS_DESC_ID)));
          
          choiceBuilder.setAction(activateTheOption(event.getChannel()));
          
          ButtonMenu menu = choiceBuilder.build();
                    
          menu.display(event.getChannel());
          
        }else {
          
          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLangage(), "infocardsOptionLongDescDisable"),
              LanguageManager.getText(server.getLangage(), INFOCARDS_DESC_ID)));
          
          choiceBuilder.setAction(disableTheOption(event.getChannel()));
          
          ButtonMenu menu = choiceBuilder.build();
          
          menu.display(event.getChannel());
        }
      }
        
      };
  }
  
  private Consumer<ReactionEmote> disableTheOption(MessageChannel messageChannel) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emote) {
        messageChannel.sendTyping().complete();
        TextChannel textChannel = Zoe.getJda().getTextChannelById(messageChannel.getId());
        
        Server server = ServerData.getServers().get(textChannel.getId());
        
        if(emote.getName().equals("✅")) {
          optionActivated = false;
          messageChannel.sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionBeenDisable")).queue();
        }else {
          messageChannel.sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionStillEnable")).queue();
        }
    }};
  }
  
  private Consumer<ReactionEmote> activateTheOption(MessageChannel messageChannel) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        messageChannel.sendTyping().complete();
        TextChannel textChannel = Zoe.getJda().getTextChannelById(messageChannel.getId());
        
        Server server = ServerData.getServers().get(textChannel.getId());
        
        if(emoteUsed.getName().equals("✅")) {
          optionActivated = true;
          messageChannel.sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionBeenActivated")).queue();
        }else {
          messageChannel.sendMessage(LanguageManager.getText(server.getLangage(), "cleanChannelOptionStillDisable")).queue();
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
  public String getChoiceText(SpellingLanguage langage) {
    String status;
    
    if(optionActivated) {
      status = LanguageManager.getText(langage, "optionEnable");
    }else {
      status = LanguageManager.getText(langage, "optionDisable");
    }
    return LanguageManager.getText(langage, description) + " : " + status;
  }

  @Override
  public String getSave() {
    return id + ":" + optionActivated;
  }

  @Override
  public void restoreSave(String save) {
    String[] saveDatas = save.split(":");
    
    optionActivated = Boolean.parseBoolean(saveDatas[1]);
  }

  public boolean isOptionActivated() {
    return optionActivated;
  }

  public void setOptionActivated(boolean optionActivated) {
    this.optionActivated = optionActivated;
  }
}
