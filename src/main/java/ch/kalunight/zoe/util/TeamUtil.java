package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.exception.ImpossibleToDeterminePositionException;
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.clash.DataPerChampion;
import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReport;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportFlexPick;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportHighEloDiff;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportHighMastery;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportHighWinrate;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportKDA;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportLittleChampionPool;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportOTP;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportPlayedGames;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportType;
import ch.kalunight.zoe.model.dangerosityreport.PickData;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.model.dto.DTO.ChampionRoleAnalysis;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.model.static_data.CustomEmote;
import ch.kalunight.zoe.model.team.AccountDataWithRole;
import ch.kalunight.zoe.repositories.ChampionRoleAnalysisRepository;
import ch.kalunight.zoe.service.analysis.ChampionRole;
import ch.kalunight.zoe.translation.LanguageManager;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPlayer;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPosition;

public class TeamUtil {

  private static final Logger logger = LoggerFactory.getLogger(TeamUtil.class);

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
  
  public static void addFlexStats(Server server, List<TeamPlayerAnalysisDataCollector> teamPlayersData, StringBuilder messageBuilder) {
    List<DataPerChampion> championsFlex = TeamUtil.getFlexPickMostPlayed(teamPlayersData, 3);

    StringBuilder flexChampionsText = new StringBuilder();

    int championToTreat;

    if(championsFlex.size() < 3) {
      championToTreat = championsFlex.size();
    }else {
      championToTreat = 3;
    }

    for(DataPerChampion flexPick : championsFlex) {
      Champion championData = Ressources.getChampionDataById(flexPick.getChampionId());

      String championString = LanguageManager.getText(server.getLanguage(), "unknown");
      if(championData != null) {
        championString = championData.getEmoteUsable() + " " + championData.getName();
      }

      flexChampionsText.append(championString + " ("); 

      DangerosityReportFlexPick flexReport = (DangerosityReportFlexPick) flexPick.getDangerosityReport(DangerosityReportType.FLEX_PICK);
      int roleToTreat = flexReport.getRolesWherePlayed().size();
      for(ChampionRole role : flexReport.getRolesWherePlayed()) {

        flexChampionsText.append(LanguageManager.getText(server.getLanguage(), TeamUtil.getChampionRoleAbrID(role)));

        roleToTreat--;
        if(roleToTreat != 0) {
          flexChampionsText.append(", ");
        }else {
          flexChampionsText.append(")");
        }
      }

      championToTreat--;
      if(championToTreat != 0) {
        flexChampionsText.append(", ");
      }else {
        break;
      }
    }

    messageBuilder.append(String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentPotentialFlexPick"), flexChampionsText.toString()));
    
    if(championsFlex.isEmpty()) {
      messageBuilder.append(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentPotentialFlexPickNotFound"));
    }
  }

  public static void addPlayersStats(Server server, List<TeamPlayerAnalysisDataCollector> teamPlayersData, StringBuilder messageBuilder) {
    Collections.sort(teamPlayersData);
    
    for(TeamPlayerAnalysisDataCollector playerToShow : teamPlayersData) {

      String translationRole = LanguageManager.getText(server.getLanguage(), TeamUtil.getChampionRoleAbrID(playerToShow.getFinalDeterminedPosition()));

      String elo = LanguageManager.getText(server.getLanguage(), "unranked");

      FullTier rank = playerToShow.getHeighestRank();

      if(rank != null) {
        String usableEmoteRank = "";
        
        if(rank != null) {
          CustomEmote emoteRank = Ressources.getTierEmote().get(rank.getTier());
          if(emoteRank != null && emoteRank.getEmote() != null) {
            usableEmoteRank = emoteRank.getEmote().getAsMention() + " ";
          }
        }
        elo = playerToShow.getHeighestRankType(server.getLanguage()) + " : " + usableEmoteRank + rank.toString(server.getLanguage());
      }

      messageBuilder.append("**" + String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentPlayerData"), translationRole,
          playerToShow.getSummoner().getName(), elo) + "**");

      messageBuilder.append("\n");

      List<DataPerChampion> champions = playerToShow.getMostPlayedChampions(3);

      int championToLoad = champions.size();
      if(championToLoad == 0) {
        messageBuilder.append("  -> *" + LanguageManager.getText(server.getLanguage(), "empty") + "*\n\n");
      }
      
      for(DataPerChampion champion : champions) {
        messageBuilder.append("  -> ");

        Champion championData = Ressources.getChampionDataById(champion.getChampionId());

        String championString = LanguageManager.getText(server.getLanguage(), "unknown");
        if(championData != null) {
          championString = championData.getEmoteUsable() + " " + championData.getName();
        }

        String winrate = LanguageManager.getText(server.getLanguage(), "unknown");
        int nbrGames = 0;
        String masteryPoint = "0";

        for(DangerosityReport report : champion.getDangerosityReports()) {
          if(report instanceof DangerosityReportHighWinrate) {
            DangerosityReportHighWinrate winrateReport = (DangerosityReportHighWinrate) report;
            winrate = DangerosityReport.POURCENTAGE_FORMAT.format(winrateReport.getWinrate());
            nbrGames = winrateReport.getNbrGames();
          }

          if(report instanceof DangerosityReportHighMastery) {
            DangerosityReportHighMastery masteryReport = (DangerosityReportHighMastery) report;
            masteryPoint = LanguageUtil.convertMasteryToReadableText(masteryReport.getRawMastery());
          } 
        }

        messageBuilder.append(String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentPlayerDataChampion"),
            championString, Integer.toString(nbrGames), winrate + "%", masteryPoint));
        championToLoad--;
        if(championToLoad != 0) {
          messageBuilder.append("\n");
        }else {
          messageBuilder.append("\n\n");
        }
      }
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
        PickData pickForThisChampion = new PickData(playerToTreat.getPlatform(),
            playerToTreat.getSummoner(), championToTreat.getChampionId(), playerToTreat.getFinalDeterminedPosition(), combinedReport);

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

      Champion championRessources = Ressources.getChampionDataById(champion.getChampionId());
      
      List<DangerosityReport> dangerosityReportList = champion.getDangerosityReports();

      dangerosityReportList.add(new DangerosityReportPlayedGames(champion.getNumberOfGame()));
      dangerosityReportList.add(new DangerosityReportHighWinrate(champion.getWinrate(), champion.getNumberOfGame()));
      dangerosityReportList.add(new DangerosityReportKDA(champion.getAverageKDA(), championRessources.getAverageKDA(), champion.getNumberOfGame()));
      dangerosityReportList.add(new DangerosityReportHighMastery(champion.getMastery()));
    }

    playerToLoad.getDangerosityReports().add(new DangerosityReportLittleChampionPool(playerToLoad.getDataPerChampions().size()));

    DataPerChampion champion = playerToLoad.getMostPlayedChampion();
    if(champion != null) {
      champion.getDangerosityReports().add(new DangerosityReportOTP(champion.getNumberOfGame() / (double) playerToLoad.getTotalNumberOfGames() * 100, champion.getNumberOfGame()));
    }
  }

  private static void createFlexReport(List<TeamPlayerAnalysisDataCollector> teamPlayersData) {
    for(Champion championToCheck : Ressources.getChampions()) {
      List<DataPerChampion> championPlayedPerRole = new ArrayList<>();
      List<ChampionRole> rolesPlayed = new ArrayList<>();
      int cumuledGames = 0;
      for(TeamPlayerAnalysisDataCollector playerToCheck : teamPlayersData) {

        DataPerChampion champion = playerToCheck.getDataPerChampionById(championToCheck.getKey());
        if(champion != null && champion.getNumberOfGame() >= DangerosityReportFlexPick.NUMBER_OF_GAME_NEEDED_TO_BE_CONSIDERED) {
          championPlayedPerRole.add(champion);
          cumuledGames += champion.getNumberOfGame();
          rolesPlayed.add(playerToCheck.getFinalDeterminedPosition());
        }
      }

      for(DataPerChampion flexChampion : championPlayedPerRole) {
        flexChampion.getDangerosityReports().add(new DangerosityReportFlexPick(rolesPlayed, cumuledGames));
      }
    }
  }

  public static void clearChampionNotInRole(List<TeamPlayerAnalysisDataCollector> playersToClear) {
    try {
      List<ChampionRoleAnalysis> championsDataRole = ChampionRoleAnalysisRepository.getAllChampionRoleAnalysis(); //TODO: Test this

      for(TeamPlayerAnalysisDataCollector player : playersToClear) {
        List<DataPerChampion> championsToDelete = new ArrayList<>();

        getChampionToDelete(championsDataRole, player, championsToDelete);

        player.getDataPerChampions().removeAll(championsToDelete);
      }
    } catch (SQLException e) {
      logger.error("Error to get championRoleAnalysis");
    }


  }

  private static void getChampionToDelete(List<ChampionRoleAnalysis> championsDataRole,
      TeamPlayerAnalysisDataCollector player, List<DataPerChampion> championsToDelete) {
    for(DataPerChampion championToMaybeDelete : player.getDataPerChampions()) {
      for(ChampionRoleAnalysis championRoleToCheck : championsDataRole) {
        if(championRoleToCheck.cra_keyChampion == championToMaybeDelete.getChampionId()) {
          if(!championRoleToCheck.cra_roles.contains(player.getFinalDeterminedPosition())) {
            championsToDelete.add(championToMaybeDelete);
          }
          break;
        }
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

  public static List<ClashPlayer> getPlayerByPosition(ClashPosition position, List<ClashPlayer> members) {
    List<ClashPlayer> membersWithTheSamePosition = new ArrayList<>();
    for(ClashPlayer member : members) {
      if(member.getPosition() == position) {
        membersWithTheSamePosition.add(member);
      }
    }
    return membersWithTheSamePosition;
  }

  public static String getTeamPositionAbrID(ClashPosition teamPosition) {
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
  
  public static String getTeamPositionId(ClashPosition teamPosition) {
    switch (teamPosition) {
    case BOTTOM:
      return "adc";
    case FILL:
      return "fillAbr";
    case JUNGLE:
      return "jungle";
    case MIDDLE:
      return "mid";
    case TOP:
      return "top";
    case UNSELECTED:
      return "unselected";
    case UTILITY:
      return "support";
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
  
  public static String getChampionRoleID(ChampionRole championRole) {
    switch (championRole) {
    case ADC:
      return "adc";
    case JUNGLE:
      return "jungle";
    case MID:
      return "mid";
    case TOP:
      return "top";
    case SUPPORT:
      return "support";
    default:
      return "Error";
    }
  }

  public static ChampionRole convertTeamPosition(ClashPosition position) {
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

  public static List<TeamPlayerAnalysisDataCollector> getTeamPlayersDataWithAnalysisDoneWithClashData(ZoePlatform platform, List<ClashPlayer> teamMembers) {
    List<TeamPlayerAnalysisDataCollector> teamPlayersData = loadAllPlayersDataWithClashData(platform, teamMembers);

    return executeTeamAnalysis(teamPlayersData);
  }

  public static List<TeamPlayerAnalysisDataCollector> loadAllPlayersDataWithClashData(ZoePlatform platform,
      List<ClashPlayer> teamMembers) {
    List<TeamPlayerAnalysisDataCollector> teamPlayersData = new ArrayList<>();

    for(ClashPlayer teamMember : teamMembers) {
      TeamPlayerAnalysisDataCollector player = new TeamPlayerAnalysisDataCollector(teamMember.getSummonerId(), platform, teamMember.getPosition());
      teamPlayersData.add(player);
      ServerThreadsManager.getDataAnalysisThread().execute(player);
    }

    TeamPlayerAnalysisDataCollector.awaitAll(teamPlayersData);
    return teamPlayersData;
  }
  
  public static List<TeamPlayerAnalysisDataCollector> getTeamPlayersDataWithAnalysisDoneWithAccountData(List<AccountDataWithRole> teamMembers) {
    List<TeamPlayerAnalysisDataCollector> teamPlayersData = loadAllPlayersDataWithAccountData(teamMembers);

    return executeTeamAnalysis(teamPlayersData);
  }

  public static List<TeamPlayerAnalysisDataCollector> loadAllPlayersDataWithAccountData(List<AccountDataWithRole> teamMembers) {
    List<TeamPlayerAnalysisDataCollector> teamPlayersData = new ArrayList<>();

    for(AccountDataWithRole teamMember : teamMembers) {
      TeamPlayerAnalysisDataCollector player = new TeamPlayerAnalysisDataCollector(teamMember.getSummoner().getSummonerId(), teamMember.getPlatform(), teamMember.getPosition());
      teamPlayersData.add(player);
      ServerThreadsManager.getDataAnalysisThread().execute(player);
    }

    TeamPlayerAnalysisDataCollector.awaitAll(teamPlayersData);
    return teamPlayersData;
  }

  private static List<TeamPlayerAnalysisDataCollector> executeTeamAnalysis(
      List<TeamPlayerAnalysisDataCollector> teamPlayersData) {
    determineRole(teamPlayersData);

    clearChampionNotInRole(teamPlayersData);

    determineDangerosity(teamPlayersData);

    return teamPlayersData;
  }

  public static List<DataPerChampion> getFlexPickMostPlayed(List<TeamPlayerAnalysisDataCollector> teamPlayersData, int numberOfWantedFlexPick) {

    List<DataPerChampion> flexChampions = new ArrayList<>();
    List<DangerosityReportFlexPick> flexPicksReport = new ArrayList<>();
    for(Champion championToCheck : Ressources.getChampions()) {
      boolean championFinded = false;
      for(TeamPlayerAnalysisDataCollector playerToCheck : teamPlayersData) {
        DataPerChampion championData = playerToCheck.getDataPerChampionById(championToCheck.getKey());
        if(championData != null) {
          for(DangerosityReport report : championData.getDangerosityReports()) {
            if(report instanceof DangerosityReportFlexPick) {
              DangerosityReportFlexPick flexPickReport = (DangerosityReportFlexPick) report;
              if(flexPickReport.getReportValue() != DangerosityReport.BASE_SCORE) {
                flexChampions.add(championData);
                flexPicksReport.add(flexPickReport);
                championFinded = true;
                break;
              }
            }
          }
        }
        if(championFinded) {
          break;
        }
      }
    }
    
    Collections.sort(flexPicksReport);
    
    List<DataPerChampion> flexChampionFiltered = new ArrayList<>();
    
    int championToGet = numberOfWantedFlexPick;
    for(DangerosityReportFlexPick flexReport : flexPicksReport) {
      
      DataPerChampion champion = getChampionByReport(flexChampions, flexReport);
      
      if(champion != null) {
        flexChampionFiltered.add(champion);
      }
      
      championToGet--;
      if(championToGet == 0) {
        break;
      }
    }

    return flexChampionFiltered;
  }
  
  public static DataPerChampion getChampionByReport(List<DataPerChampion> championsToSearch, DangerosityReport report) {
    for(DataPerChampion champion : championsToSearch) {
      DangerosityReport reportToCheck = champion.getDangerosityReport(report.getReportType());
      if(reportToCheck.equals(report)) {
        return champion;
      }
    }
    return null;
  }

  public static List<PickData> getHeighestDangerosity(List<PickData> picksData, int numberOfPickWanted) {
    Collections.sort(picksData);
    
    if(numberOfPickWanted > picksData.size()) {
      return picksData;
    }else {
      List<PickData> picksToReturn = new ArrayList<>();
      int i = 0;
      for(PickData pickData : picksData) {
        if(i < numberOfPickWanted) {
          picksToReturn.add(pickData);
        }else {
          break;
        }
        
        i++;
      }
      return picksToReturn;
    }
  }
  
  
  public static List<PickData> getHeighestDangerosityAllTeam(List<TeamPlayerAnalysisDataCollector> players, int numberOfPickWanted) {
    
    List<PickData> picksData = new ArrayList<>();
    
    for(TeamPlayerAnalysisDataCollector player : players) {
      picksData.addAll(player.getPicksCompiledData());
    }
    
    return getHeighestDangerosity(picksData, numberOfPickWanted);
  }

}
