package ch.kalunight.zoe.service.analysis;

import javax.annotation.Nullable;

public enum ChampionRole {
  TOP("TOP", "SOLO", 1),
  JUNGLE("JUNGLE", "NONE", 2),
  MID("MIDDLE", "SOLO", 3),
  ADC("BOTTOM", "DUO_CARRY", 4),
  SUPPORT("BOTTOM", "DUO_SUPPORT", 5);
  
  private String lane;
  private String role;
  private int order;
  
  private ChampionRole(String lane, String role, int order) {
    this.lane = lane;
    this.role = role;
    this.order = order;
  }

  @Nullable
  public static ChampionRole getChampionRoleWithLaneAndRole(String lane, String role) {
    if(lane.equals(TOP.lane) && role.equals(TOP.role)){
      return TOP;
    }
    
    if(lane.equals(MID.lane) && role.equals(MID.role)) {
      return MID;
    }
    
    if(lane.equals(JUNGLE.lane) || role.equals(JUNGLE.role)) {
      return JUNGLE;
    }
    
    if(lane.equals(ADC.lane)) {
      if(role.equals(ADC.role)) {
        return ADC;
      }
      
      if(role.equals(SUPPORT.role)) {
        return SUPPORT;
      }
    }
    
    return null;
  }
  
  public String getLane() {
    return lane;
  }

  public String getRole() {
    return role;
  }

  public int getOrder() {
    return order;
  }
  
}
