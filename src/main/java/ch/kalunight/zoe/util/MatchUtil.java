package ch.kalunight.zoe.util;

import net.rithms.riot.constant.Platform;

public class MatchUtil {

  private MatchUtil() {
    //hide default constructor
  }
  
  public static String convertV4GameIdIntoV5Id(long gameId, Platform platform) {
    return platform.getId() + "_" + gameId;
  }
  
}
