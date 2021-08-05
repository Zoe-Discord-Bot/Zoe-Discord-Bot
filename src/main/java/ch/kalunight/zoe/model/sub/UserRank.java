package ch.kalunight.zoe.model.sub;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.util.Ressources;

public enum UserRank {
  DEV(0, "devName", "554670096103112776", false, 0),
  STAFF(1, "staffName", "872075875745677403", false, 0),
  SUB_TIER_3(2, "subTier3Name", "870672597673705493", true, 25),
  SUB_TIER_2(3, "subTier2Name", "870672480946233365", true, 10),
  SUB_TIER_1(4, "subTier1Name", "870672322032447538", true, 5),
  EXCEPTIONNAL_FEATURES_ACCESS(5, "exceptionnalAccessName", "872088918370181150", false, 0);
  
  private long id;
  private String nameId;
  private String roleId;
  private boolean supporter;
  private long value;
  
  private UserRank(int id, String nameId, String roleId, boolean supporter, int value) {
    this.id = id;
    this.nameId = nameId;
    this.roleId = roleId;
    this.supporter = supporter;
    this.value = value;
  }

  public String getEmoteWithUser(DTO.ZoeUser user) {
    switch(this) {
    case DEV:
      return ":tools:";
    case EXCEPTIONNAL_FEATURES_ACCESS:
      return ":ticket:";
    case STAFF:
      return ":shield:";
    case SUB_TIER_1:
    case SUB_TIER_2:
    case SUB_TIER_3:
      long monthsSupported = user.zoeUser_fullMonthSupported + 1;
      if(monthsSupported == 1) {
        return Ressources.getZoeSub1Month().getUsableEmote();
      }else if(monthsSupported == 2) {
        return Ressources.getZoeSub2Months().getUsableEmote();
      }else if(monthsSupported > 2 && monthsSupported < 6) {
        return Ressources.getZoeSub3Months().getUsableEmote();
      }else if(monthsSupported > 5 && monthsSupported < 9) {
        return Ressources.getZoeSub6Months().getUsableEmote();
      }else if(monthsSupported > 8 && monthsSupported < 12) {
        return Ressources.getZoeSub9Months().getUsableEmote();
      }else {
        return Ressources.getZoeSub1Year().getUsableEmote();
      }
    default:
      return "";
    }
  }
  
  public static UserRank getUserRankByDiscordRoleId(String discordRoleId) {
    for(UserRank rank : UserRank.values()) {
      if(rank.getRoleId().equals(discordRoleId)) {
        return rank;
      }
    }
    
    return null;
  }
  
  public static UserRank getUserRankByRoleId(long roleId) {
    for(UserRank rank : UserRank.values()) {
      if(rank.getId() == roleId) {
        return rank;
      }
    }
    
    return null;
  }
  
  public long getId() {
    return id;
  }

  public String getNameId() {
    return nameId;
  }

  public String getRoleId() {
    return roleId;
  }

  public boolean isSupporter() {
    return supporter;
  }

  public long getValue() {
    return value;
  }
  
}