package ch.kalunight.zoe.util;

import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.model.player_data.Player;

public class NameConversion {

  private NameConversion() {}

  public static String convertStringToTinyString(String stringToConvert) {
    if(stringToConvert.length() > 14) {
      return stringToConvert.substring(0, 12) + "..";
    }
    return stringToConvert;
  }

  public static String convertGameQueueIdToString(int id) {
    switch(id) {
      case 0:
        return "gameTypeCustom";
      case 400:
        return "gameType5v5DraftSR";
      case 420:
        return "gameType5v5RankSoloQSR";
      case 430:
        return "gameType5v5BlindSR";
      case 440:
        return "gameType5v5RankedFlexSR";
      case 450:
        return "gameTypeARAM";
      case 460:
        return "gameType3v3BlindTT";
      case 470:
        return "gameType3v3RankFlexTT";
      case 700:
        return "gameType5v5ClashSR";
      case 800:
      case 810:
      case 820:
        return "gameTypeCoopVsIATT";
      case 830:
      case 840:
      case 850:
        return "gameTypeCoopVsIASR";
      case 900:
        return "gameTypeClassicUrf";
      default:
        return "gameTypeUnknown";
    }
  }

  public static List<String> getListNameOfPlayers(List<Player> players) {
    List<String> playersName = new ArrayList<>();

    for(int j = 0; j < players.size(); j++) {
      String name = "";
      if(players.get(j).isMentionnable()) {
        name = players.get(j).getDiscordUser().getAsMention();
      } else {
        name = players.get(j).getDiscordUser().getName();
      }
      playersName.add(name);
    }
    return playersName;
  }
}
