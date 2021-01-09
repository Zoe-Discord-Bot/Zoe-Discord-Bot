package ch.kalunight.zoe.model.clash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.MatchReceiver;
import ch.kalunight.zoe.model.TeamPositionRated;
import ch.kalunight.zoe.model.dto.SavedChampionsMastery;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.service.analysis.ChampionRole;
import ch.kalunight.zoe.util.ClashUtil;
import ch.kalunight.zoe.util.LoLQueueIdUtil;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.clash.constant.TeamPosition;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.constant.Platform;

public class TeamPlayerAnalysisDataCollector implements Runnable {

  private static final Set<Integer> selectedQueuesId = Collections.synchronizedSet(new HashSet<>());

  private static final Logger logger = LoggerFactory.getLogger(TeamPlayerAnalysisDataCollector.class);

  protected static final List<String> summonerIdInWork = Collections.synchronizedList(new ArrayList<>());

  private String summonerId;

  private Platform platform;

  private ChampionRole clashSelectedPosition;
  
  private ChampionRole finalDeterminedPosition;

  private List<TeamPositionRated> determinedPositions;

  private List<WinratePerChampion> winratePerChampions;

  private SavedChampionsMastery masteryPerChampions;

  private Set<LeagueEntry> eloOfThePlayer;

  private AtomicInteger nbrTop = new AtomicInteger();
  private AtomicInteger nbrJng = new AtomicInteger();
  private AtomicInteger nbrMid = new AtomicInteger();
  private AtomicInteger nbrAdc = new AtomicInteger();
  private AtomicInteger nbrSup = new AtomicInteger();

  private AtomicInteger nbrMatch = new AtomicInteger();

  static {
    selectedQueuesId.add(LoLQueueIdUtil.NORMAL_DRAFT_QUEUE_ID);
    selectedQueuesId.add(LoLQueueIdUtil.RANKED_SOLO_QUEUE_ID);
    selectedQueuesId.add(LoLQueueIdUtil.NORMAL_BLIND_QUEUE_ID);
    selectedQueuesId.add(LoLQueueIdUtil.RANKED_FLEX_QUEUE_ID);
    selectedQueuesId.add(LoLQueueIdUtil.CLASH_GAME_QUEUE_ID);
  }

  public TeamPlayerAnalysisDataCollector(String summonerId, Platform platform, TeamPosition position) {
    this.summonerId = summonerId;
    this.platform = platform;
    this.winratePerChampions = new ArrayList<>();
    clashSelectedPosition = ClashUtil.convertTeamPosition(position);
    summonerIdInWork.add(summonerId);
  }

  @Override
  public void run() {
    try {
      loadAllData();
    } catch (RiotApiException e) {
      logger.error("Riot api exception in the collection of clash member data", e);
    } catch (Exception e) {
      logger.error("Unexpected exception in the collection of clash member data", e);
    }finally {
      summonerIdInWork.remove(summonerId);
    }
  }

  public void loadAllData() throws RiotApiException {

    SavedSummoner summoner = Zoe.getRiotApi().getSummonerWithRateLimit(platform, summonerId, false);

    MatchReceiver matchReceiver = RiotRequest.getAllMatchsByQueue(summonerId, platform, false, selectedQueuesId);

    for(SavedMatch match : matchReceiver.matchs) {
      SavedMatchPlayer playerInMatch = match.getSavedMatchPlayerByAccountId(summoner.getAccountId());

      collectRoleData(playerInMatch);

      collectWinrateData(playerInMatch, match);
    }

    masteryPerChampions = Zoe.getRiotApi().getChampionMasteriesBySummonerWithRateLimit(platform, summonerId, false);

    eloOfThePlayer = Zoe.getRiotApi().getLeagueEntriesBySummonerIdWithRateLimit(platform, summonerId);

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
    WinratePerChampion winrate = getWinrateByChampion(playerInMatch.getChampionId());

    if(winrate == null) {
      winrate = new WinratePerChampion(playerInMatch.getChampionId(), new ArrayList<>());
    }

    winrate.getMatchs().add(match);
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

  private WinratePerChampion getWinrateByChampion(int championId) {
    for(WinratePerChampion checkWinratePerChampion : winratePerChampions) {
      if(checkWinratePerChampion.getChampionId() == championId) {
        return checkWinratePerChampion;
      }
    }
    return null;
  }

  public static void awaitAll(List<String> summonersToWait) {

    boolean needToWait;

    do {
      needToWait = false;
      for(String summonerToWait : summonersToWait) {
        if(summonerIdInWork.contains(summonerToWait)) {
          needToWait = true;
          break;
        }
      }

      if(needToWait) {
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
          logger.error("Thread as been interupt when waiting Match Worker !", e);
          Thread.currentThread().interrupt();
        }
      }
    }while(needToWait);
  }

  public String getSummonerId() {
    return summonerId;
  }

  public Platform getPlatform() {
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
    return null;
  }

  public List<WinratePerChampion> getWinratePerChampions() {
    return winratePerChampions;
  }

  public SavedChampionsMastery getMasteryPerChampions() {
    return masteryPerChampions;
  }

  public Set<LeagueEntry> getEloOfThePlayer() {
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
  
}
