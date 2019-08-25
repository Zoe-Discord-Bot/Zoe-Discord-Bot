package ch.kalunight.zoe.model.config.option;


import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;

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
        
        if(optionActivated == false) {
          
          Message message = event.getTextChannel().sendMessage("Option in activation : **" + description + "**\n\n"
              + "This option will let player add them self in the system with the command ``>create player``, ``>delete player`` "
              + "and this will activate the command ``>register``.\n\n"
              + ""
              + ":white_check_mark: : Activate this option.\n"
              + ":negative_squared_cross_mark: : Cancel the activation.").complete();
          
          message.addReaction("✅").queue();
          message.addReaction("❌").queue();
          
          waiter.waitForEvent(MessageReactionAddEvent.class,
              e -> true, e -> checkOption(message, event.getMember()), 1, TimeUnit.MINUTES, () -> endTime());
        }else {
          
          
          
          
          
        }
      }
    };
  }
  
  private void checkOption(Message message, Member member) {
    
    for(MessageReaction reaction: message.getReactions()) {
      if(reaction.getReactionEmote().getName().equals("✅")) {
        
        for(User userReaction : reaction.getUsers()) {
          if(userReaction.equals(member.getUser())) {
            
            
            
          }
        }
      }
    }
    
  }
  
  
  private void activateOption(Message message) {
    
  }
  
  private void endTime() {
    
    
    
  }

  @Override
  public String getChoiceText() {
    // TODO Auto-generated method stub
    return null;
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
