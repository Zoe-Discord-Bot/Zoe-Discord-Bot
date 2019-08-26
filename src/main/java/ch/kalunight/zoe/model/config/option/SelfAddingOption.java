package ch.kalunight.zoe.model.config.option;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import net.dv8tion.jda.core.entities.Message;

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
              + ":negative_squared_cross_mark: : Cancel the activation.");
          
          choiceBuilder.setTimeout(2, TimeUnit.MINUTES);
          
        }else {
          
          
          
          
          
        }
      }
    };
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
