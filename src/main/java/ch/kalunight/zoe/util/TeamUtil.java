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
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.clash.DataPerChampion;
import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReport;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportFlexPick;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportHighEloDiff;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportHighMastery;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportHighWinrate;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportLittleChampionPool;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportOTP;
import ch.kalunight.zoe.model.dangerosityreport.PickData;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.service.analysis.ChampionRole;
import ch.kalunight.zoe.translation.LanguageManager;
import net.rithms.riot.api.endpoints.clash.constant.TeamPosition;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeamMember;

public class TeamUtil {

  private TeamUtil() {
    //hide public default class
  }

  public static void determineRole(List<TeamPlayerAnalysisDataCollector> playersToDetermine) {
    if(playersToDetermine.size() == 5) {

      Map<ChampionRole, List<TeamPlayerAnalysisDataCollector>> roleSelected = Collections.synchronizedMap(new EnumMap<>(ChampionRole.class));
      filterPlayerBySelectedRole(playersToDetermine, roleSelected);

      List<ChampionRole> rolesToDefine = defineSelectedRoleAndGetRoleToDefine(roleSelected);

      List<TeamPlayerAnalysisDataCollector> playersToStillDetermine = getPlayerToDetermine(playersToDetermine);

      giveToEachPlayerMostPlayedRole(rolesToDefine, playersToStillDetermine);

      giveRemainingRoleRandomly(rolesToDefine, playersToStillDetermine);

    }else {
      throw new ImpossibleToDeterminePositionException("The given team is not equal to 5 ! Impossible to determine role !");
    }
  }


  private static void giveRemainingRoleRandomly(List<ChampionRole> rolesToDefine,
      List<TeamPlayerAnalysisDataCollector> playersToStillDetermine) {
    if(!rolesToDefine.isEmpty()) {
      int playerIdToSelect = 0;
      for(ChampionRole role : rolesToDefine) {
        playersToStillDetermine.get(playerIdToSelect).setFinalDeterminedPosition(role);
        playerIdToSelect++;
      }
    }
  }

  private static void giveToEachPlayerMostPlayedRole(List<ChampionRole> rolesToDefine,
      List<TeamPlayerAnalysisDataCollector> playersToStillDetermine) {
    int rolesToRemove = rolesToDefine.size();

    while(rolesToRemove > 0) {
      TeamPlayerAnalysisDataCollector heighestRatioPlayer = null;
      ChampionRole heighestRationRole = null;
      for(TeamPlayerAnalysisDataCollector playerToDetermine : playersToStillDetermine) {

        for(ChampionRole roleToCheck : rolesToDefine) {
          if(heighestRatioPlayer == null || (playerToDetermine.getDeterminedPositionsByRole(roleToCheck) != null 
              && playerToDetermine.getDeterminedPositionsByRole(roleToCheck).getRatioOfPlay() > heighestRatioPlayer.getDeterminedPositionsByRole(heighestRationRole).getRatioOfPlay())) {
            heighestRatioPlayer = playerToDetermine;
            heighestRationRole = roleToCheck;
          }
        }
      }

      if(heighestRatioPlayer != null) {
        heighestRatioPlayer.setFinalDeterminedPosition(heighestRationRole);
        playersToStillDetermine.remove(heighestRatioPlayer);
        rolesToDefine.remove(heighestRationRole);
      }

      rolesToRemove--;
    }
  }

  private static List<TeamPlayerAnalysisDataCollector> getPlayerToDetermine(
      List<TeamPlayerAnalysisDataCollector> playersToDetermine) {
    List<TeamPlayerAnalysisDataCollector> playersToStillDetermine = new ArrayList<>();

    for(TeamPlayerAnalysisDataCollector playerToDetermine : playersToDetermine) {
      if(playerToDetermine.getFinalDeterminedPosition() == null) {
        playersToStillDetermine.add(playerToDetermine);
      }
    }
    return playersToStillDetermine;
  }

  private static List<ChampionRole> defineSelectedRoleAndGetRoleToDefine(
      Map<ChampionRole, List<TeamPlayerAnalysisDataCollector>> roleSelected) {
    List<ChampionRole> rolesToDefine = new ArrayList<>();
    rolesToDefine.addAll(Arrays.asList(ChampionRole.values()));

    for(Entry<ChampionRole, List<TeamPlayerAnalysisDataCollector>> roleToCheck : roleSelected.entrySet()) {
      if(roleToCheck.getValue().size() == 1) {
        roleToCheck.getValue().get(0).setFinalDeterminedPosition(roleToCheck.getKey());
        rolesToDefine.remove(roleToCheck.getKey());
      }
    }
    return rolesToDefine;
  }

  private static void filterPlayerBySelectedRole(List<TeamPlayerAnalysisDataCollector> playersToDetermine,
      Map<ChampionRole, List<TeamPlayerAnalysisDataCollector>> roleSelected) {
    for(ChampionRole role : ChampionRole.values()) {
      List<TeamPlayerAnalysisDataCollector> selectedRole = new ArrayList<>();
      roleSelected.put(role, selectedRole);

      for(TeamPlayerAnalysisDataCollector playerToDetermine : playersToDetermine) {
        if(playerToDetermine.getClashSelectedPosition() == role) {
          selectedRole.add(playerToDetermine);
        }
      }
    }
  }

  public static void determineDangerosity(List<TeamPlayerAnalysisDataCollector> teamPlayersData) {

    int totalRankValueOfPlayers = 0;
    int numberOfPlayerRanked = 0;

    for(TeamPlayerAnalysisDataCollector playerToLoad : teamPlayersData) {
      generatePerChampionReport(playerToLoad);

      FullTier eloOfPlayer = playerToLoad.getHeighestRank();
      try {
        if(eloOfPlayer != null) {
          totalRankValueOfPlayers += eloOfPlayer.value();
          numberOfPlayerRanked++;
        }
      }catch (NoValueRankException e) {
        // This player has not a valid rank. So we just skip it.
      }
    }

    manageHighEloReport(teamPlayersData, totalRankValueOfPlayers, numberOfPlayerRanked);

    createFlexReport(teamPlayersData);
    
    generateChampionPick(teamPlayersData);
  }

  private static void generateChampionPick(List<TeamPlayerAnalysisDataCollector> teamPlayersData) {
    for(TeamPlayerAnalysisDataCollector playerToTreat : teamPlayersData) {
      for(DataPerChampion championToTreat : playerToTreat.getDataPerChampions()) {
        List<DangerosityReport> combinedReport = new ArrayList<>();
        combinedReport.addAll(championToTreat.getDangerosityReports());
        combinedReport.addAll(playerToTreat.getDangerosityReports());
        PickData pickForThisChampion = new PickData(playerToTreat.getSummonerId(), playerToTreat.getPlatform(),
            playerToTreat.getSummoner(), championToTreat.getChampionId(), combinedReport);
        
        playerToTreat.getPicksCompiledData().add(pickForThisChampion);
      }
    }
  }

  private static void manageHighEloReport(List<TeamPlayerAnalysisDataCollector> teamPlayersData,
      int totalRankValueOfPlayers, int numberOfPlayerRanked) {
    if(numberOfPlayerRanked != 0) {
      FullTier averageRank = new FullTier(totalRankValueOfPlayers / numberOfPlayerRanked); 

      for(TeamPlayerAnalysisDataCollector playerToTreat : teamPlayersData) {
        FullTier heighestRank = playerToTreat.getHeighestRank();
        if(heighestRank != null) {
          playerToTreat.getDangerosityReports().add(new DangerosityReportHighEloDiff(averageRank, heighestRank));
        }
      }
    }
  }

  private static void generatePerChampionReport(TeamPlayerAnalysisDataCollector playerToLoad) {
    for(DataPerChampion champion : playerToLoad.getDataPerChampions()) {

      List<DangerosityReport> dangerosityReportList = champion.getDangerosityReports();

      dangerosityReportList.add(new DangerosityReportHighMastery(champion.getMastery()));
      dangerosityReportList.add(new DangerosityReportHighWinrate(champion.getWinrate(), champion.getNumberOfGame()));
    }

    playerToLoad.getDangerosityReports().add(new DangerosityReportLittleChampionPool(playerToLoad.getDataPerChampions().size()));

    DataPerChampion champion = playerToLoad.getMostPlayedChampion();
    if(champion != null) {
      champion.getDangerosityReports().add(new DangerosityReportOTP(champion.getNumberOfGame() / (double) (playerToLoad.getTotalNumberOfGames() * 100)));
    }
  }

  private static void createFlexReport(List<TeamPlayerAnalysisDataCollector> teamPlayersData) {
    for(Champion championToCheck : Ressources.getChampions()) {
      List<DataPerChampion> championPlayedPerRole = new ArrayList<>();
      List<ChampionRole> rolesPlayed = new ArrayList<>();
      for(TeamPlayerAnalysisDataCollector playerToCheck : teamPlayersData) {

        DataPerChampion champion = playerToCheck.getDataPerChampionById(championToCheck.getKey());
        if(champion != null) {
          championPlayedPerRole.add(champion);
          rolesPlayed.add(playerToCheck.getFinalDeterminedPosition());
        }
      }

      for(DataPerChampion flexChampion : championPlayedPerRole) {
        flexChampion.getDangerosityReports().add(new DangerosityReportFlexPick(rolesPlayed));
      }
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

  public static String getChampionRoleAbrID(ChampionRole championRole) {
    switch (championRole) {
    case ADC:
      return "adcAbr";
    case JUNGLE:
      return "jungleAbr";
    case MID:
      return "midAbr";
    case TOP:
      return "topAbr";
    case SUPPORT:
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
