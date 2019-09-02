package ch.kalunight.zoe.model.config.option;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
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
        
        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();
        
        choiceBuilder.setEventWaiter(waiter);
        choiceBuilder.addChoices("✅","❌");
        choiceBuilder.addUsers(event.getAuthor());
        choiceBuilder.setFinalAction(finalAction());

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);
        
        if(!optionActivated) {

          choiceBuilder.setText("Option in activation : **" + description + "**\n\n"
              + "This option will allow all members of the server to add/delete them self in the "
              + "system with the command ``>create player``, ``>delete player``, ``>add accountToPlayer``, ``>remove accountToPlayer`` "
              + "and this will activate the command ``>register``.\n\n"
              + ":white_check_mark: : Activate this option.\n"
              + ":x: : Cancel the activation.");
          
          choiceBuilder.setAction(activateTheOption(event.getChannel()));
          
          ButtonMenu menu = choiceBuilder.build();
                    
          menu.display(event.getChannel());
          
        }else {
          
          choiceBuilder.setText("Option you want to disable : **" + description + "**\n\n"
              + "All members will not be longer allowed to add/delete them self in the system. "
              + "The command ``>register`` will be disable.\n"
              + "**Are you sure to disable this option ?**\n\n"
              + ":white_check_mark: : **Disable** the option\n"
              + ":x: : Cancel the disable procedure");
          
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
        
        if(emote.getName().equals("✅")) {
          optionActivated = false;
          messageChannel.sendMessage("Right, the option has been disabled.").queue();
        }else {
          messageChannel.sendMessage("Right, the option is still enable.").queue();
        }
    }};
  }
  
  private Consumer<ReactionEmote> activateTheOption(MessageChannel messageChannel) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        
        messageChannel.sendTyping().complete();
        
        if(emoteUsed.getName().equals("✅")) {
          optionActivated = true;
          messageChannel.sendMessage("Right, the option has been activated.").queue();
        }else {
          messageChannel.sendMessage("Right, the option is still disable.").queue();
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
