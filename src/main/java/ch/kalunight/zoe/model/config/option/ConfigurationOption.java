package ch.kalunight.zoe.model.config.option;

import java.sql.SQLException;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.model.dto.DTO;

public abstract class ConfigurationOption {

  protected static final String NO_VALUE_REPRESENTATION = "null";
  
  protected long guildId;
  protected String description;
  
  public ConfigurationOption(long guildId, String description) {
    this.guildId = guildId;
    this.description = description;
  }
  
  /**
   * Consumer who create a new interface for the user to change the option
   * @param waiter of Zoe. Used to wait user action.
   * @return Consumer who is the interface
   */
  public abstract Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter, DTO.Server server);

  /**
   * Pattern -> Description : Status (Enabled/Disabled)
   * @return description of the option and his status
   * @throws SQLException
   */
  public abstract String getChoiceText(String langage) throws SQLException;
  
  public String getDescription() {
    return description;
  }
}
