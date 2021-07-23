package ch.kalunight.zoe.model.config.option;

import java.sql.SQLException;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.dto.DTO;

public abstract class ConfigurationOption {

  protected static final String NO_VALUE_REPRESENTATION = "null";
  
  protected static final String DISABLE_ID = "disable";
  protected static final String CANCEL_ID = "cancel";
  protected static final String VALIDATE_ID = "validate";
  protected static final String TFT_ID = "tft";
  protected static final String FLEX_ID = "flex";
  protected static final String SOLOQ_ID = "soloq";
  
  protected long guildId;
  protected String description;
  protected OptionCategory category;
  
  public ConfigurationOption(long guildId, String description, OptionCategory category) {
    this.guildId = guildId;
    this.description = description;
    this.category = category;
  }
  
  /**
   * Consumer who create a new interface for the user to change the option
   * @param waiter of Zoe. Used to wait user action.
   * @return Consumer who is the interface
   */
  public abstract Consumer<CommandGuildDiscordData> getChangeConsumer(EventWaiter waiter, DTO.Server server);

  /**
   * Pattern -> Description : Status (Enabled/Disabled)
   * @return description of the option and his status
   * @throws SQLException
   */
  public abstract String getChoiceText(String langage) throws SQLException;
  
  public String getDescription() {
    return description;
  }
  
  public OptionCategory getCategory() {
    return category;
  }
}
