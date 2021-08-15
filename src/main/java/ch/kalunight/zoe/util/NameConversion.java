package ch.kalunight.zoe.util;

import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;

public class NameConversion {

  private NameConversion() {}

  public static String convertStringToTinyString(String stringToConvert) {
    if(stringToConvert.length() > 14) {
      return stringToConvert.substring(0, 12) + "..";
    }
    return stringToConvert;
  }

  public static String convertGameQueueIdToString(GameQueueType gameQueueType) {
    switch(gameQueueType) {
      case CUSTOM:
        return "gameTypeCustom";
      case NORMAL_5X5_DRAFT:
        return "gameType5v5DraftSR";
      case RANKED_SOLO_5X5:
        return "gameType5v5RankSoloQSR";
      case NORMAL_5V5_BLIND_PICK:
        return "gameType5v5BlindSR";
      case RANKED_FLEX_SR:
        return "gameType5v5RankedFlexSR";
      case ARAM:
        return "gameTypeARAM";
      case NORMAL_3X3_BLIND_PICK:
        return "gameType3v3BlindTT";
      case RANKED_FLEX_TT:
        return "gameType3v3RankFlexTT";
      case CLASH:
        return "gameType5v5ClashSR";
      case BOT_3X3_INTERMEDIATE:
      case BOT_3X3_INTRO:
      case BOT_3X3_BEGINNER:
        return "gameTypeCoopVsIATT";
      case BOT_5X5_INTRO:
      case BOT_5X5_BEGINNER:
      case BOT_5X5_INTERMEDIATE:
        return "gameTypeCoopVsIASR";
      case ALL_RANDOM_URF:
        return "gameTypeClassicUrf";
      default:
        return "gameTypeUnknown";
    }
  }
}
