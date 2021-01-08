package ch.kalunight.zoe.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.google.common.base.CharMatcher;

import ch.kalunight.zoe.exception.ImpossibleToDeterminePositionException;
import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.service.analysis.ChampionRole;
import ch.kalunight.zoe.translation.LanguageManager;
import net.rithms.riot.api.endpoints.clash.constant.TeamPosition;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeamMember;

public class ClashUtil {

  private ClashUtil() {
    //hide public default class
  }

  public static void determineRole(List<TeamPlayerAnalysisDataCollector> playersToDetermine) {
    if(playersToDetermine.size() == 5) {

      Map<ChampionRole, List<TeamPlayerAnalysisDataCollector>> roleSelected = Collections.synchronizedMap(new EnumMap<>(ChampionRole.class));
      for(ChampionRole role : ChampionRole.values()) {
        List<TeamPlayerAnalysisDataCollector> selectedRole = new ArrayList<>();
        roleSelected.put(role, selectedRole);

        for(TeamPlayerAnalysisDataCollector playerToDetermine : playersToDetermine) {
          if(playerToDetermine.getClashSelectedPosition() == role) {
            selectedRole.add(playerToDetermine);
          }
        }
      }
      
      List<ChampionRole> rolesToDefine = new ArrayList<>();
      rolesToDefine.addAll(Arrays.asList(ChampionRole.values()));
      
      for(Entry<ChampionRole, List<TeamPlayerAnalysisDataCollector>> roleToCheck : roleSelected.entrySet()) {
        if(roleToCheck.getValue().size() == 1) {
          roleToCheck.getValue().get(0).setFinalDeterminedPosition(roleToCheck.getKey());
          rolesToDefine.remove(roleToCheck.getKey());
        }
      }

      List<TeamPlayerAnalysisDataCollector> playersToStillDetermine = new ArrayList<>();
      
      for(TeamPlayerAnalysisDataCollector playerToDetermine : playersToDetermine) {
        if(playerToDetermine.getFinalDeterminedPosition() == null) {
          playersToStillDetermine.add(playerToDetermine);
        }
      }
      
      
      
    }else {
      throw new ImpossibleToDeterminePositionException("The given team is not equal to 5 ! Impossible to determine role !");
    }
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

  public static ChampionRole convertTeamPosition(TeamPosition position) {
    switch (position) {
    case BOTTOM:
      return ChampionRole.ADC;
    case JUNGLE:
      return ChampionRole.JUNGLE;
    case MIDDLE:
      return ChampionRole.MID;
    case TOP:
      return ChampionRole.TOP;
    case UTILITY:
      return ChampionRole.SUPPORT;
    default:
      return null;
    }
  }

}
