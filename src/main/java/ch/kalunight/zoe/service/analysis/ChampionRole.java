package ch.kalunight.zoe.service.analysis;

import javax.annotation.Nullable;

public enum ChampionRole {
  TOP("TOP_LANE", "SOLO"),
  JUNGLE("JUNGLE", "NONE"),
  MID("MID_LANE", "SOLO"),
  ADC("BOT_LANE", "DUO_CARRY"),
  SUPPORT("BOT_LANE", "DUO_SUPPORT");
  
  private String lane;
  private String role;
  
  private ChampionRole(String lane, String role) {
    this.lane = lane;
    this.role = role;
  }

  @Nullable
  public static ChampionRole getChampionRoleWithLaneAndRole(String lane, String role) {
    if(lane.equals(TOP.lane)){
      return TOP;
    }
    
    if(lane.equals(MID.lane)) {
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
}
