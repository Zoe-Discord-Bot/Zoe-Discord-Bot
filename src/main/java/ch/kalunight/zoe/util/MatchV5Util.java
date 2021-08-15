package ch.kalunight.zoe.util;

import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;

public class MatchV5Util {

  public static String convertMatchV4IdToMatchV5Id(long matchId, LeagueShard regionOfTheGame) {
    return regionOfTheGame.getValue() + "_" + matchId;
  }
  
}
