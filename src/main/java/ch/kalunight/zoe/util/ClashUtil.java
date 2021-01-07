package ch.kalunight.zoe.util;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.CharMatcher;

import ch.kalunight.zoe.translation.LanguageManager;
import net.rithms.riot.api.endpoints.clash.constant.TeamPosition;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeamMember;

public class ClashUtil {

  private ClashUtil() {
    //hide public default class
  }
  
  /**
   * Utility method to parse the "nameKeySecondary" of the clash tournament API.
   * @param language
   * @param nameKeySecondary
   * @return the user friendly
   */
  public static String parseDayId(String language, String nameKeySecondary) {
    String dayNumber = CharMatcher.inRange('0', '9').retainFrom(nameKeySecondary);
    return String.format(LanguageManager.getText(language, "dayNumber"), dayNumber);
  }
  
  public static List<ClashTeamMember> getPlayerByPosition(TeamPosition position, List<ClashTeamMember> members) {
    List<ClashTeamMember> membersWithTheSamePosition = new ArrayList<>();
    for(ClashTeamMember member : members) {
      if(member.getTeamPosition() == position) {
        membersWithTheSamePosition.add(member);
      }
    }
    return membersWithTheSamePosition;
  }
  
  public static String getTeamPositionAbrID(TeamPosition teamPosition) {
    switch (teamPosition) {
    case BOTTOM:
      return "adcAbr";
    case FILL:
      return "fillAbr";
    case JUNGLE:
      return "jungleAbr";
    case MIDDLE:
      return "midAbr";
    case TOP:
      return "topAbr";
    case UNSELECTED:
      return "unselected";
    case UTILITY:
      return "supportAbr";
    default:
      return "Error";
    }
  }
  
}
