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

  public static String convertGameModeToString(String stringToConvert) {

    if(stringToConvert.equals("CLASSIC")) {
      return "Faille de l'invocateur";
    } else if(stringToConvert.equals("GAMEMOEDEX")) {
      return "Mode de jeu en rotation";
    } else {
      return stringToConvert;
    }
  }

  public static String convertGameQueueIdToString(int id) {
    switch(id) {
      case 0:
        return "Custom game";
      case 400:
        return "5v5 Draft Pick | Summoner's Rift";
      case 420:
        return "5v5 Ranked Solo | Summoner's Rift";
      case 430:
        return "5v5 Blind Pick | Summoner's Rift";
      case 440:
        return "5v5 Ranked Flex | Summoner's Rift";
      case 450:
        return "ARAM | Howling Abyss";
      case 460:
        return "3v3 Blind Pick | Twisted Treeline";
      case 470:
        return "3v3 Ranked Flex | Twisted Treeline";
      case 700:
        return "Clash | Summoner's Rift";
      case 800:
      case 810:
      case 820:
        return "Coop vs IA | Twisted Treeline";
      case 830:
      case 840:
      case 850:
        return "Coop vs IA | Summoner's Rift";
      case 900:
        return "URF | Summoner's Rift";
      case 1200:
        return "Raid du Nexus | Summoner's Rift";
      default:
        return "Temporary mode";
    }
  }

  public static String convertGameTypeToString(String stringToConvert) {

    if(stringToConvert.equals("MATCHED_GAME")) {
      return "Matchmaking";
    } else {
      return stringToConvert;
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
