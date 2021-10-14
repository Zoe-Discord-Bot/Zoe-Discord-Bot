package ch.kalunight.zoe.service.analysis;

import javax.annotation.Nullable;

import no.stelar7.api.r4j.basic.constants.types.lol.LaneType;
import no.stelar7.api.r4j.basic.constants.types.lol.RoleType;

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
  public static ChampionRole getChampionRoleWithLaneAndRole(LaneType lane, RoleType role) {
    if(lane.equals(LaneType.TOP)){
      return TOP;
    }
    
    if(lane.equals(LaneType.MID)) {
      return MID;
    }
    
    if(lane.equals(LaneType.JUNGLE)) {
      return JUNGLE;
    }
    
    if(lane.equals(LaneType.BOT)) {
      if(role.equals(RoleType.DUO_CARRY) || role.equals(RoleType.CARRY)) {
        return ADC;
      }
      
      if(role.equals(RoleType.DUO_SUPPORT) || role.equals(RoleType.SUPPORT)) {
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
