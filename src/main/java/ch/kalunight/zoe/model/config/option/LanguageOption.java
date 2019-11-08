package ch.kalunight.zoe.model.config.option;

import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.model.static_data.SpellingLangage;

public class LanguageOption extends ConfigurationOption {

  public LanguageOption(String id, String description) {
    super("language", "Language of the bot");
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter) {
    return new Consumer<CommandEvent>() {

      @Override
      public void accept(CommandEvent t) {
        // TODO Auto-generated method stub
        
      }};
  }

  @Override
  public String getChoiceText(SpellingLangage langage) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getSave() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void restoreSave(String save) {
    // TODO Auto-generated method stub

  }

}
