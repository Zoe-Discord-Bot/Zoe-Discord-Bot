package ch.kalunight.zoe.model.sub;

public enum UserRank {
  DEV(0, "devName"),
  STAFF(1, "staffName"),
  SUB_TIER_3(2, "subTier3Name"),
  SUB_TIER_2(3, "subTier2Name"),
  SUB_TIER_1(4, "subTier1Name"),
  EXCEPTIONNAL_FEATURES_ACCESS(5, "exceptionnalAccessName");
  
  private int id;
  private String nameId;
  
  private UserRank(int id, String nameId) {
    this.id = id;
    this.nameId = nameId;
  }

  public int getId() {
    return id;
  }

  public String getNameId() {
    return nameId;
  }
  
}