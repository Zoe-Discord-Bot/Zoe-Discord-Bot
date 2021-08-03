package ch.kalunight.zoe.model.sub;

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

  public static UserRank getUserRankByRoleId(String roleId) {
    for(UserRank rank : UserRank.values()) {
      if(rank.getRoleId().equals(roleId)) {
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