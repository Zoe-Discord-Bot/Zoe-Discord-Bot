package ch.kalunight.zoe.model.config.option;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;

public class SelfAddingOption extends ConfigurationOption {

  private boolean optionActivated;

  public SelfAddingOption() {
    super("self_adding", "Everyone can add/delete them self in the system");
    optionActivated = false;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter) {
    return new Consumer<CommandEvent>() {
      
      @Override
      public void accept(CommandEvent event) {
        
        if(!optionActivated) {
          ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();
          
          choiceBuilder.setEventWaiter(waiter);
          choiceBuilder.addChoices("✅","❌");
          choiceBuilder.addUsers(event.getAuthor());
          choiceBuilder.setText("Option in activation : **" + description + "**\n\n"
              + "This option will let player add them self in the system with the command ``>create player``, ``>delete player`` "
              + "and this will activate the command ``>register``.\n\n"
              + ":white_check_mark: : Activate this option.\n"
              + ":x: : Cancel the activation.");
          
          choiceBuilder.setTimeout(2, TimeUnit.MINUTES);
          
          choiceBuilder.setAction(activateTheOption(event.getChannel()));
          choiceBuilder.setFinalAction(finalAction());
          
          ButtonMenu menu = choiceBuilder.build();
                    
          menu.display(event.getChannel());
          
        }else {
          
          
          
          
          
        }
      }
    };
  }
  
  public Consumer<ReactionEmote> activateTheOption(MessageChannel messageChannel) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        
        if(emoteUsed.getName().equals("✅")) {
          messageChannel.sendMessage("Good").queue();
        }
      }};
  }
  
  public Consumer<Message> finalAction(){
    return new Consumer<Message>() {

      @Override
      public void accept(Message t) {
        // TODO Auto-generated method stub
        
      }};
  }

  @Override
  public String getChoiceText() {
    String status;
    
    if(optionActivated) {
      status = "Enable";
    }else {
      status = "Disable";
    }
    return description + " : " + status;
  }

  @Override
  public String getSave() {
    return id + " : " + optionActivated;
  }

  @Override
  public void restoreSave(String save) {
    String[] saveDatas = save.split(":");
    
    optionActivated = Boolean.parseBoolean(saveDatas[1]);
  }
  
  public boolean isOptionActivated() {
    return optionActivated;
  }

}
