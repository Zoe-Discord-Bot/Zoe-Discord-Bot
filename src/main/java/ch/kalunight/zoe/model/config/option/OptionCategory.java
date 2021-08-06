package ch.kalunight.zoe.model.config.option;

import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.model.config.ServerConfiguration;
import net.dv8tion.jda.api.entities.Emoji;

public enum OptionCategory {
  GENERAL("generalCategoryDescription", ":one:", "🌍", "generalCategoryName", "generalCategoryId"),
  INFOCHANNEL("infochannelCategoryDescription", ":two:", "🗒", "infochannelCategoryName", "InfoChannelCategoryId"),
  RANKCHANNEL("rankchannelCategoryDescription", ":three:", "🏆", "rankchannelCategoryName", "RankChannelCategoryId"),
  USER_MANAGEMENT("userManagementCategoryDescription", ":four:", "🔧", "userManagementCategoryName", "UserManagementCatgoryId"),
  FEATURES("featuresCategoryDescription", ":five:", "✅", "featuresCategoryName", "featuresCategoryId");
  
  private String descriptionId;
  private String emote;
  private Emoji emoji;
  private String nameId;
  private String id;
  
  private OptionCategory(String descriptionId, String emote, String emoji, String nameId, String id) {
    this.descriptionId = descriptionId;
    this.emote = emote;
    this.emoji = Emoji.fromUnicode(emoji);
    this.nameId = nameId;
    this.id = id;
  }
  
  public static List<ConfigurationOption> getOptionsById(String id, ServerConfiguration config){
    OptionCategory selectedCategory = null;
    
    for(OptionCategory category : OptionCategory.values()) {
      if(category.getId().equals(id)) {
        selectedCategory = category;
        break;
      }
    }
    
    if(selectedCategory == null) {
      return new ArrayList<>();
    }
    
    return config.getConfigurationsInFunctionOfCategory(selectedCategory);
  }

  public String getDescriptionId() {
    return descriptionId;
  }

  public String getEmote() {
    return emote;
  }

  public Emoji getEmoji() {
    return emoji;
  }

  public String getNameId() {
    return nameId;
  }

  public String getId() {
    return id;
  }
}