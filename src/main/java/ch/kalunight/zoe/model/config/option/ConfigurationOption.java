package ch.kalunight.zoe.model.config.option;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.ZoeSubscriptionListener;
import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.translation.LanguageManager;

public abstract class ConfigurationOption {

  public static final String NO_VALUE_REPRESENTATION = "null";
  
  public static final String DISABLE_ID = "disable";
  public static final String CANCEL_ID = "cancel";
  public static final String VALIDATE_ID = "validate";
  public static final String TFT_ID = "tft";
  public static final String FLEX_ID = "flex";
  public static final String SOLOQ_ID = "soloq";
  
  protected long guildId;
  protected String name;
  protected String description;
  protected OptionCategory category;
  protected boolean earlyAccessOption;
  
  public ConfigurationOption(long guildId, String name, String description, OptionCategory category,
      boolean earlyAccessOption) {
    this.guildId = guildId;
    this.name = name;
    this.description = description;
    this.category = category;
    this.earlyAccessOption = earlyAccessOption;
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
  public abstract String getBaseChoiceText(String langage) throws SQLException;
  
  public String getChoiceText(String language, boolean serverBoosted) throws SQLException {

    String baseChoiceText = getBaseChoiceText(language);
    
    if(!earlyAccessOption 
        || ZoeSubscriptionListener.END_EARLY_ACCESS_PHASE_FEATURES.isBefore(LocalDateTime.now())) {
      return baseChoiceText;
    }
    
    if(serverBoosted) {
      return LanguageManager.getText(baseChoiceText, "configEarlyAccessFeature")
          + " " + baseChoiceText;
    }
    
    return LanguageManager.getText(language, "configEarlyAccessFeatureNeedRight")
        + " " + baseChoiceText;
  }
  
  public String getName() {
    return name;
  }
  
  public String getDescription() {
    return description;
  }
  
  public OptionCategory getCategory() {
    return category;
  }
  
  public boolean hasAccessToTheFeatures(boolean serverBoosted) {
    return !earlyAccessOption
        || serverBoosted 
        || ZoeSubscriptionListener.END_EARLY_ACCESS_PHASE_FEATURES.isBefore(LocalDateTime.now());
  }
}
