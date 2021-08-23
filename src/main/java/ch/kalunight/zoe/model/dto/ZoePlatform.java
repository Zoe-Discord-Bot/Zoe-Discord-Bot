package ch.kalunight.zoe.model.dto;

import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;

public enum ZoePlatform {
  EUW("euw", LeagueShard.EUW1),
  EUNE("eune", LeagueShard.EUN1),
  BR("br", LeagueShard.BR1),
  JP("jp", LeagueShard.JP1),
  KR("kr", LeagueShard.KR),
  LAN("lan", LeagueShard.LA1),
  LAS("las", LeagueShard.LA2),
  NA("na", LeagueShard.NA1),
  OCE("oce", LeagueShard.OC1),
  TR("tr", LeagueShard.TR1),
  RU("ru", LeagueShard.RU);
  
  private String dbName;
  
  private LeagueShard leagueShard;
  
  private ZoePlatform(String dbName, LeagueShard leagueShard) {
    this.dbName = dbName;
    this.leagueShard = leagueShard;
  }

  public ZoePlatform getZoePlatformByLeagueShard(LeagueShard leagueShard) {
    for(ZoePlatform platformToCheck : ZoePlatform.values()) {
      if(platformToCheck.leagueShard.equals(leagueShard)) {
        return platformToCheck;
      }
    }
    return null;
  }
  
  public String getDbName() {
    return dbName;
  }
  
  public String getShowableName() {
    return dbName.toUpperCase();
  }

  public LeagueShard getLeagueShard() {
    return leagueShard;
  }
  
}