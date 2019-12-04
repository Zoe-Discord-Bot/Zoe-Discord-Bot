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
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;

public class InfoCardOption extends ConfigurationOption {

  private static final String INFOCARDS_DESC_ID = "infocardsOptionDesc";
  
  private boolean optionActivated;
  
  public InfoCardOption(long guildId) {
    super(guildId, INFOCARDS_DESC_ID);
    this.optionActivated = true;
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

          choiceBuilder.setText(String.format(LanguageManager.getText(server.serv_language,
              "infocardsOptionLongDescEnable"), LanguageManager.getText(server.serv_language, INFOCARDS_DESC_ID)));
          
          choiceBuilder.setAction(activateTheOption(event.getChannel()));
          
          ButtonMenu menu = choiceBuilder.build();
                    
          menu.display(event.getChannel());
          
        }else {
          
          choiceBuilder.setText(String.format(LanguageManager.getText(server.serv_language, "infocardsOptionLongDescDisable"),
              LanguageManager.getText(server.serv_language, INFOCARDS_DESC_ID)));
          
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
        
        Server server = ServerData.getServers().get(textChannel.getGuild().getId());
        
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
        
        Server server = ServerData.getServers().get(textChannel.getGuild().getId());
        
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
