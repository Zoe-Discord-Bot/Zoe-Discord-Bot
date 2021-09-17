package ch.kalunight.zoe.util;

import ch.kalunight.zoe.model.dto.ZoePlatform;

public class MatchV5Util {

  public static String convertMatchV4IdToMatchV5Id(long matchId, ZoePlatform regionOfTheGame) {
    return regionOfTheGame.getLeagueShard().getValue() + "_" + matchId;
  }
  
}
