package ch.kalunight.zoe.model.clash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.TeamPositionRated;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReport;
import ch.kalunight.zoe.model.dangerosityreport.PickData;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;
import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.player_data.Tier;
import ch.kalunight.zoe.service.analysis.ChampionRole;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.TeamUtil;
import ch.kalunight.zoe.util.request.RiotRequest;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPosition;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;

public class TeamPlayerAnalysisDataCollector implements Runnable, Comparable<TeamPlayerAnalysisDataCollector>{

  private static final List<GameQueueType> selectedQueuesId = Collections.synchronizedList(new ArrayList<>());

  private static final Logger logger = LoggerFactory.getLogger(TeamPlayerAnalysisDataCollector.class);

  protected static final List<TeamPlayerAnalysisDataCollector> summonerIdInWork = Collections.synchronizedList(new ArrayList<>());

  private String summonerId;

  private SavedSummoner summoner;

  private ZoePlatform platform;

  private ChampionRole clashSelectedPosition;

  private ChampionRole finalDeterminedPosition;

  private List<TeamPositionRated> determinedPositions;

  private List<DataPerChampion> dataPerChampions;

  private List<LeagueEntry> eloOfThePlayer;

  private List<DangerosityReport> dangerosityReports;

  private List<PickData> picksCompiledData;

  private AtomicInteger nbrTop = new AtomicInteger();
  private AtomicInteger nbrJng = new AtomicInteger();
  private AtomicInteger nbrMid = new AtomicInteger();
  private AtomicInteger nbrAdc = new AtomicInteger();
  private AtomicInteger nbrSup = new AtomicInteger();

  private AtomicInteger nbrMatch = new AtomicInteger();

  static {
    selectedQueuesId.add(GameQueueType.NORMAL_5X5_DRAFT);
    selectedQueuesId.add(GameQueueType.NORMAL_5V5_BLIND_PICK);
    selectedQueuesId.add(GameQueueType.RANKED_SOLO_5X5);
    selectedQueuesId.add(GameQueueType.RANKED_FLEX_SR);
    selectedQueuesId.add(GameQueueType.CLASH);
  }

  public TeamPlayerAnalysisDataCollector(String summonerId, ZoePlatform platform, ClashPosition position) {
    this.summonerId = summonerId;
    this.platform = platform;
    this.dataPerChampions = new ArrayList<>();
    if(position != null) {
      clashSelectedPosition = TeamUtil.convertTeamPosition(position);
    }
    this.dangerosityReports = new ArrayList<>();
    this.picksCompiledData = new ArrayList<>();
    summonerIdInWork.add(this);
  }

  public TeamPlayerAnalysisDataCollector(String summonerId, SavedSummoner summoner,
      ZoePlatform platform, ClashPosition position) {
    this.summonerId = summonerId;
    this.summoner = summoner;
    this.platform = platform;
    this.dataPerChampions = new ArrayList<>();
    if(position != null) {
      clashSelectedPosition = TeamUtil.convertTeamPosition(position);
    }
    this.dangerosityReports = new ArrayList<>();
    this.picksCompiledData = new ArrayList<>();
    summonerIdInWork.add(this);
  }
  
  @Override
  public void run() {
    try {
      loadAllData();
    } catch (APIResponseException e) {
      logger.error("Riot api exception in the collection of clash member data", e);
    } catch (Exception e) {
      logger.error("Unexpected exception in the collection of clash member data", e);
    }finally {
      summonerIdInWork.remove(this);
    }
  }

  public void loadAllData() throws APIResponseException {

    summoner = Zoe.getRiotApi().getSummonerBySummonerId(platform, summonerId);

    List<SavedMatch> matchs = RiotRequest.getMatchHistoryOfLastMonth(platform, summoner, selectedQueuesId, new ArrayList<>());

    for(SavedMatch match : matchs) {
      SavedMatchPlayer playerInMatch = null;
      
      for(SavedMatchPlayer participantToCheck : match.getPlayers()) {
        if(participantToCheck.getSummonerId().equals(summoner.getSummonerId())) {
          playerInMatch = participantToCheck;
          break;
        }
      }
      
      if(playerInMatch == null) {
        logger.error("Summoner not detected inside a game where he should be! LeagueShard {} GameId {} SummonerName {}"
            + " | Skipping...", platform.getShowableName(), match.getGameId(), summoner.getSummonerId());
        continue;
      }

      collectRoleData(playerInMatch);

      collectWinrateData(playerInMatch, match);
    }

    List<SavedSimpleMastery> masteryPerChampions = Zoe.getRiotApi().getChampionMasteryBySummonerId(platform, summonerId);
    
    for(SavedSimpleMastery mastery : masteryPerChampions) {
      DataPerChampion dataOfChampion = getDataByChampion(mastery.getChampionId());
      if(dataOfChampion != null) {
        dataOfChampion.setMastery(mastery);
      }
    }

    eloOfThePlayer = Zoe.getRiotApi().getLeagueEntryBySummonerId(platform, summoner.getSummonerId());

    treatPosition();
  }

  private void treatPosition() {
    determinedPositions = new ArrayList<>();

    if(nbrTop.get() != 0) {
      determinedPositions.add(new TeamPositionRated(ChampionRole.TOP, nbrTop.get() / (double) nbrMatch.get() * 100.0));
    }

    if(nbrJng.get() != 0) {
      determinedPositions.add(new TeamPositionRated(ChampionRole.JUNGLE, nbrJng.get() / (double) nbrMatch.get() * 100.0));
    }

    if(nbrMid.get() != 0) {
      determinedPositions.add(new TeamPositionRated(ChampionRole.MID, nbrMid.get() / (double) nbrMatch.get() * 100.0));
    }

    if(nbrAdc.get() != 0) {
      determinedPositions.add(new TeamPositionRated(ChampionRole.ADC, nbrAdc.get() / (double) nbrMatch.get() * 100.0));
    }

    if(nbrSup.get() != 0) {
      determinedPositions.add(new TeamPositionRated(ChampionRole.SUPPORT, nbrSup.get() / (double) nbrMatch.get() * 100.0));
    }

    Collections.sort(determinedPositions);
  }

  private void collectWinrateData(SavedMatchPlayer playerInMatch, SavedMatch match) {
    DataPerChampion dataForTheChampion = getDataByChampion(playerInMatch.getChampionId());

    if(dataForTheChampion == null) {
      dataForTheChampion = new DataPerChampion(playerInMatch.getChampionId(), new ArrayList<>());
      dataPerChampions.add(dataForTheChampion);
    }

    dataForTheChampion.getMatchs().add(match);
  }

  private void collectRoleData(SavedMatchPlayer playerInMatch) {
    ChampionRole role = ChampionRole.getChampionRoleWithLaneAndRole(playerInMatch.getLane(), playerInMatch.getRole());
    
    if(role != null) {
      switch(role) {
      case ADC:
        nbrAdc.incrementAndGet();
        break;
      case JUNGLE:
        nbrJng.incrementAndGet();
        break;
      case MID:
        nbrMid.incrementAndGet();
        break;
      case SUPPORT:
        nbrSup.incrementAndGet();
        break;
      case TOP:
        nbrTop.incrementAndGet();
        break;
      }

      nbrMatch.incrementAndGet();
    }
  }

  private DataPerChampion getDataByChampion(int championId) {
    for(DataPerChampion checkWinratePerChampion : dataPerChampions) {
      if(checkWinratePerChampion.getChampionId() == championId) {
        return checkWinratePerChampion;
      }
    }
    return null;
  }

  public static void awaitAll(List<TeamPlayerAnalysisDataCollector> summonersToWait) {

    boolean needToWait;

    do {
      needToWait = false;
      for(TeamPlayerAnalysisDataCollector summonerToWait : summonersToWait) {
        if(summonerIdInWork.contains(summonerToWait)) {
          needToWait = true;
          break;
        }
      }

      if(needToWait) {
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
          logger.error("Thread as been interupt when waiting TeamPlayerAnalysisDataCollector !", e);
          Thread.currentThread().interrupt();
        }
      }
    }while(needToWait);
  }

  @Override
  public int compareTo(TeamPlayerAnalysisDataCollector objectToCompare) {

    if(objectToCompare.getDeterminedPositions() == null) {
      return 0;
    }

    if(objectToCompare.getFinalDeterminedPosition().getOrder() > finalDeterminedPosition.getOrder()) {
      return -1;
    }else if (objectToCompare.getFinalDeterminedPosition().getOrder() < finalDeterminedPosition.getOrder()) {
      return 1;
    }

    return 0;
  }

  public List<DangerosityReport> getDangerosityReports() {
    return dangerosityReports;
  }

  public String getSummonerId() {
    return summonerId;
  }

  public ZoePlatform getPlatform() {
    return platform;
  }

  public List<TeamPositionRated> getDeterminedPositions() {
    return determinedPositions;
  }

  public TeamPositionRated getDeterminedPositionsByRole(ChampionRole role) {
    for(TeamPositionRated determinedPosition : determinedPositions) {
      if(determinedPosition.getChampionRole() == role) {
        return determinedPosition;
      }
    }
    logger.debug("No game detected with the role {} for the player {}", role, summoner.getName());
    return new TeamPositionRated(role, 0);
  }

  public int getTotalNumberOfGames() {
    int nbrOfGames = 0;
    for(DataPerChampion data : dataPerChampions) {
      nbrOfGames += data.getNumberOfGame();
    }

    return nbrOfGames;
  }

  public DataPerChampion getMostPlayedChampion() {
    DataPerChampion championSelected = null;

    for(DataPerChampion champion : dataPerChampions) {
      if(championSelected == null || championSelected.getNumberOfGame() < champion.getNumberOfGame()) {
        championSelected = champion;
      }
    }

    return championSelected;
  }

  public List<DataPerChampion> getDataPerChampions() {
    return dataPerChampions;
  }

  public DataPerChampion getDataPerChampionById(int championId) {
    for(DataPerChampion champion : dataPerChampions) {
      if(champion.getChampionId() == championId) {
        return champion;
      }
    }
    return null;
  }
  
  public List<DataPerChampion> getMostPlayedChampions(int numberOfChampionsWanted){
    List<DataPerChampion> championWanted = new ArrayList<>();
    
    Collections.sort(dataPerChampions);
    
    for(DataPerChampion dataPerChampion : dataPerChampions) {
      if(championWanted.size() < numberOfChampionsWanted) {
        championWanted.add(dataPerChampion);
      }else {
        break;
      }
    }
    return championWanted;
  }

  public FullTier getHeighestRank() {
    FullTier fullTier = null;
    try {
      
      if(eloOfThePlayer == null) {
        return null;
      }
      
      for(LeagueEntry entry : eloOfThePlayer) {
        FullTier currentTestedElo = new FullTier(entry);
        if((!currentTestedElo.getTier().equals(Tier.UNKNOWN) && !currentTestedElo.getTier().equals(Tier.UNRANKED))
            && (fullTier == null || fullTier.value() < currentTestedElo.value())) {
          fullTier = currentTestedElo;
        }
      }
    }catch (NoValueRankException e) {
      return null;
    }
    return fullTier;
  }
  
  public String getHeighestRankType(String lang) {
    FullTier fullTier = null;
    LeagueEntry heighestEntry = null;
    try {
      for(LeagueEntry entry : eloOfThePlayer) {
        FullTier currentTestedElo = new FullTier(entry);
        if((!currentTestedElo.getTier().equals(Tier.UNKNOWN) && !currentTestedElo.getTier().equals(Tier.UNRANKED))
            && (fullTier == null || fullTier.value() < currentTestedElo.value())) {
          fullTier = currentTestedElo;
          heighestEntry = entry;
        }
      }
    }catch (NoValueRankException e) {
      return null;
    }
    
    if(heighestEntry == null) {
      return null;
    }
    
    if(GameQueueConfigId.getGameQueueIdWithQueueType(heighestEntry.getQueueType()).equals(GameQueueConfigId.SOLOQ)) {
      return LanguageManager.getText(lang, GameQueueConfigId.SOLOQ.getNameId());
    }else {
      return LanguageManager.getText(lang, GameQueueConfigId.FLEX.getNameId());
    }
  }

  public List<LeagueEntry> getEloOfThePlayer() {
    return eloOfThePlayer;
  }

  public ChampionRole getClashSelectedPosition() {
    return clashSelectedPosition;
  }

  public ChampionRole getFinalDeterminedPosition() {
    return finalDeterminedPosition;
  }

  public void setFinalDeterminedPosition(ChampionRole finalDeterminedPosition) {
    this.finalDeterminedPosition = finalDeterminedPosition;
  }

  public SavedSummoner getSummoner() {
    return summoner;
  }

  public List<PickData> getPicksCompiledData() {
    return picksCompiledData;
  }
}
