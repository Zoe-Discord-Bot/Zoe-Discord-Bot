package ch.kalunight.zoe.model.config.option;

import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

public abstract class ConfigurationOption {

  protected String description;
  
  public ConfigurationOption(String description) {
    this.setDescription(description);
  }
  
  public abstract Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter);

  /**
   * Pattern -> Description : Status (Enabled/Disabled)
   * @return description of the option and his status
   */
  public abstract String getChoiceText();
  
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
  
}
