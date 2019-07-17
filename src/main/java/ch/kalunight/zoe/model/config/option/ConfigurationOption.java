package ch.kalunight.zoe.model.config.option;

import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

public abstract class ConfigurationOption {

  protected static final String NO_VALUE_REPRESENTATION = "null";
  
  protected String id;
  protected String description;
  
  public ConfigurationOption(String id, String description) {
    this.description = description;
    this.id = id;
  }
  
  /**
   * Consumer who create a new interface for the user to change the option
   * @param waiter of Zoe. Used to wait user action.
   * @return Consumer who the interface
   */
  public abstract Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter);

  /**
   * Pattern -> Description : Status (Enabled/Disabled)
   * @return description of the option and his status
   */
  public abstract String getChoiceText();
  
  /**
   * Get save of the option <br>
   * Pattern -> id:data1:data2:data3:...
   * @return String representation of the option
   */
  public abstract String getSave();
  
  /**
   * Read and restore the option with the string representation given by {@link ConfigurationOption#getSave()}.
   */
  public abstract void restoreSave(String save);
  
  /**
   * Check if the given save is for this option.
   * @param save to check
   * @return true if the given save is for this option
   */
  public boolean isTheOption(String save) {
    return save.split(":")[0].equals(id);
  }
  
  public String getDescription() {
    return description;
  }
}
