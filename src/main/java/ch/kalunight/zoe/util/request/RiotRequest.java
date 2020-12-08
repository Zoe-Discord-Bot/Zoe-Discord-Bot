package ch.kalunight.zoe.util.request;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.KDAReceiver;
import ch.kalunight.zoe.model.WinRateReceiver;
import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.player_data.Rank;
import ch.kalunight.zoe.model.player_data.Tier;
import ch.kalunight.zoe.model.static_data.Mastery;
import ch.kalunight.zoe.service.match.MatchKDAReceiverWorker;
import ch.kalunight.zoe.service.match.MatchReceiverWorker;
import ch.kalunight.zoe.service.match.MatchWinrateReceiverWorker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.Ressources;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.constant.Platform;

public class RiotRequest {
  
  private static final Logger logger = LoggerFactory.getLogger(RiotRequest.class);

  private static final DecimalFormat df = new DecimalFormat("###.#");
  
  private static final Random random = new Random();

  private RiotRequest() {}

  public static FullTier getSoloqRank(String summonerId, Platform region) {

    Set<LeagueEntry> listLeague;
    try {
      listLeague = Zoe.getRiotApi().getLeagueEntriesBySummonerIdWithRateLimit(region, summonerId);
    } catch(RiotApiException e) {
      logger.info("Error with riot api : {}", e.getMessage());
      return new FullTier(Tier.UNKNOWN, Rank.UNKNOWN, 0);
    }

    Tier rank = Tier.UNRANKED;
    Rank tier = Rank.UNRANKED;
    int leaguePoints = 0;

    for(LeagueEntry leaguePosition : listLeague) {
      if(leaguePosition.getQueueType().equals("RANKED_SOLO_5x5")) {
        rank = Tier.valueOf(leaguePosition.getTier());
        tier = Rank.valueOf(leaguePosition.getRank());
        leaguePoints = leaguePosition.getLeaguePoints();
      }
    }

    return new FullTier(rank, tier, leaguePoints);
  }
  
  public static FullTier getFlexRank(String summonerId, Platform region) {

    Set<LeagueEntry> listLeague;
    try {
      listLeague = Zoe.getRiotApi().getLeagueEntriesBySummonerIdWithRateLimit(region, summonerId);
    } catch(RiotApiException e) {
      logger.info("Error with riot api : {}", e.getMessage());
      return new FullTier(Tier.UNKNOWN, Rank.UNKNOWN, 0);
    }

    Tier rank = Tier.UNRANKED;
    Rank tier = Rank.UNRANKED;
    int leaguePoints = 0;

    for(LeagueEntry leaguePosition : listLeague) {
      if(leaguePosition.getQueueType().equals("RANKED_FLEX_SR")) {
        rank = Tier.valueOf(leaguePosition.getTier());
        tier = Rank.valueOf(leaguePosition.getRank());
        leaguePoints = leaguePosition.getLeaguePoints();
      }
    }

    return new FullTier(rank, tier, leaguePoints);
  }
  
  public static LeagueEntry getLeagueEntrySoloq(String summonerId, Platform region) {
    Set<LeagueEntry> listLeague;
    try {
      listLeague = Zoe.getRiotApi().getLeagueEntriesBySummonerIdWithRateLimit(region, summonerId);
    } catch(RiotApiException e) {
      logger.info("Error with riot api : {}", e.getMessage());
      return null;
    }

    for(LeagueEntry leaguePosition : listLeague) {
      if(leaguePosition.getQueueType().equals("RANKED_SOLO_5x5")) {
        return leaguePosition;
      }
    }
    return null;
  }
  
  public static LeagueEntry getLeagueEntryFlex(String summonerId, Platform region) {
    Set<LeagueEntry> listLeague;
    try {
      listLeague = Zoe.getRiotApi().getLeagueEntriesBySummonerIdWithRateLimit(region, summonerId);
    } catch(RiotApiException e) {
      logger.info("Error with riot api : {}", e.getMessage());
      return null;
    }

    for(LeagueEntry leaguePosition : listLeague) {
      if(leaguePosition.getQueueType().equals("RANKED_FLEX_SR")) {
        return leaguePosition;
      }
    }
    return null;
  }

  public static String getWinrateLastMonthWithGivenChampion(String summonerId, Platform region,
      int championKey, String language, boolean forceRefreshCache) {

    SavedSummoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerWithRateLimit(region, summonerId, forceRefreshCache);
    } catch(RiotApiException e) {
      logger.warn("Impossible to get the summoner : {}", e.getMessage());
      return LanguageManager.getText(language, "unknown");
    }

    List<MatchReference> referencesMatchList;
    try {
      referencesMatchList = getMatchHistoryOfLastMonthWithTheGivenChampion(region, championKey, summoner);
    } catch(RiotApiException e) {
      logger.info("Can't acces to history : {}", e.getMessage());
      return LanguageManager.getText(language, "unknown");
    }

    if(referencesMatchList.isEmpty()) {
      return LanguageManager.getText(language, "firstGame");
    }

    AtomicBoolean gameLoadingConflict = new AtomicBoolean(false);
    WinRateReceiver winRateReceiver = new WinRateReceiver();
    
    for(MatchReference matchReference : referencesMatchList) {
      MatchWinrateReceiverWorker matchWorker = new MatchWinrateReceiverWorker(winRateReceiver, gameLoadingConflict, matchReference, region, summoner);
      ServerThreadsManager.getMatchsWorker(region).execute(matchWorker);
    }

    MatchWinrateReceiverWorker.awaitAll(referencesMatchList);

    if(gameLoadingConflict.get()) {
      try {
        TimeUnit.SECONDS.sleep(random.nextInt((10 - 2) + 1) + 2l); //Max 10 min 2
      } catch (InterruptedException e) {
        logger.error("gameLoadingConflict wait process got interupted !", e);
        Thread.currentThread().interrupt();
      }
    }

    int nbrWins = winRateReceiver.win.intValue();
    int nbrGames = nbrWins + winRateReceiver.loose.intValue();

    if(nbrGames == 0) {
      return LanguageManager.getText(language, "firstGame");
    } else if(nbrWins == 0) {
      return "0% (" + nbrWins + "W/" + (nbrGames - nbrWins) + "L)";
    }

    return df.format((nbrWins / (double) nbrGames) * 100) + "% (" + nbrWins + "W/" + (nbrGames - nbrWins) + "L)";
  }

  private static List<MatchReference> getMatchHistoryOfLastMonthWithTheGivenChampion(Platform region, int championKey, SavedSummoner summoner)
      throws RiotApiException {
    final List<MatchReference> referencesMatchList = new ArrayList<>();

    LocalDateTime beginTimeToHit = LocalDateTime.now().minusMonths(1);
    Timestamp timeStampToHit = Timestamp.valueOf(beginTimeToHit);
    
    Set<Integer> championToFilter = new HashSet<>();
    championToFilter.add(championKey);

    boolean allMatchReceived = false;
    
    int startIndex = -100;
    
    do {
      startIndex += 100;
      MatchList matchList = null;

      try {
        matchList = Zoe.getRiotApi().getMatchListByAccountIdWithRateLimit(region, summoner.getAccountId(), championToFilter, null, null,
            -1, -1, startIndex, -1);
        if(matchList != null && matchList.getMatches() != null) {
          for(MatchReference matchToCheck : matchList.getMatches()) {
            final Timestamp matchTimeStamp = new Timestamp(matchToCheck.getTimestamp());
            if(matchTimeStamp.after(timeStampToHit)) {
              referencesMatchList.add(matchToCheck);
            }else {
              allMatchReceived = true;
              break;
            }
          }
          
          if(matchList.getMatches().size() != 100) {
            allMatchReceived = true;
          }
        }else {
          allMatchReceived = true;
        }
      } catch(RiotApiException e) {
        logger.debug("Impossible to get matchs history : {}", e.getMessage());
        if(e.getErrorCode() != RiotApiException.DATA_NOT_FOUND) {
          throw e;
        }
      }

    }while(!allMatchReceived);
    
    return referencesMatchList;
  }
  
  public static KDAReceiver getKDALastMonthOneChampionOnly(String summonerId, Platform region, int championId, boolean forceRefreshCache) {

    SavedSummoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerWithRateLimit(region, summonerId, forceRefreshCache);
    } catch(RiotApiException e) {
      logger.warn("Impossible to get the summoner : {}", e.getMessage());
      return null;
    }

    List<MatchReference> referencesMatchList;
    try {
      referencesMatchList = getMatchHistoryOfLastMonthWithTheGivenChampion(region, championId, summoner);
    } catch(RiotApiException e) {
      logger.info("Can't acces to history : {}", e.getMessage());
      return null;
    }

    if(referencesMatchList.isEmpty()) {
      return null;
    }

    AtomicBoolean gameLoadingConflict = new AtomicBoolean(false);
    KDAReceiver kdaReceiver = new KDAReceiver();
    
    for(MatchReference matchReference : referencesMatchList) {
      MatchKDAReceiverWorker matchWorker = new MatchKDAReceiverWorker(kdaReceiver, gameLoadingConflict, matchReference, region, summoner);
      ServerThreadsManager.getMatchsWorker(region).execute(matchWorker);
    }

    MatchReceiverWorker.awaitAll(referencesMatchList);

    return kdaReceiver;
  }
  
  public static KDAReceiver getKDALastMonth(String summonerId, Platform region, boolean forceRefreshCache) {

    SavedSummoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerWithRateLimit(region, summonerId, forceRefreshCache);
    } catch(RiotApiException e) {
      logger.warn("Impossible to get the summoner : {}", e.getMessage());
      return null;
    }

    List<MatchReference> referencesMatchList;
    try {
      referencesMatchList = getMatchHistoryOfLastMonth(region, summoner);
    } catch(RiotApiException e) {
      logger.info("Can't acces to history : {}", e.getMessage());
      return null;
    }

    if(referencesMatchList.isEmpty()) {
      return null;
    }

    AtomicBoolean gameLoadingConflict = new AtomicBoolean(false);
    KDAReceiver kdaReceiver = new KDAReceiver();
    
    for(MatchReference matchReference : referencesMatchList) {
      MatchKDAReceiverWorker matchWorker = new MatchKDAReceiverWorker(kdaReceiver, gameLoadingConflict, matchReference, region, summoner);
      ServerThreadsManager.getMatchsWorker(region).execute(matchWorker);
    }

    MatchReceiverWorker.awaitAll(referencesMatchList);

    return kdaReceiver;
  }
  
  private static List<MatchReference> getMatchHistoryOfLastMonth(Platform region, SavedSummoner summoner)
      throws RiotApiException {
    final List<MatchReference> referencesMatchList = new ArrayList<>();

    LocalDateTime beginTimeToHit = LocalDateTime.now().minusMonths(1);
    Timestamp timeStampToHit = Timestamp.valueOf(beginTimeToHit);
    
    Set<Integer> championToFilter = new HashSet<>();

    boolean allMatchReceived = false;
    
    int startIndex = -100;
    
    do {
      startIndex += 100;
      MatchList matchList = null;

      try {
        matchList = Zoe.getRiotApi().getMatchListByAccountIdWithRateLimit(region, summoner.getAccountId(), championToFilter, null, null,
            -1, -1, startIndex, -1);
        if(matchList != null && matchList.getMatches() != null) {
          for(MatchReference matchToCheck : matchList.getMatches()) {
            final Timestamp matchTimeStamp = new Timestamp(matchToCheck.getTimestamp());
            if(matchTimeStamp.after(timeStampToHit)) {
              referencesMatchList.add(matchToCheck);
            }else {
              allMatchReceived = true;
              break;
            }
          }
          
          if(matchList.getMatches().size() != 100) {
            allMatchReceived = true;
          }
        }else {
          allMatchReceived = true;
        }
      } catch(RiotApiException e) {
        logger.debug("Impossible to get matchs history : {}", e.getMessage());
        if(e.getErrorCode() != RiotApiException.DATA_NOT_FOUND) {
          throw e;
        }
      }

    }while(!allMatchReceived);
    
    return referencesMatchList;
  }

  public static String getMasterysScore(String summonerId, int championId, Platform platform, boolean forceRefreshCache) {
    SavedSimpleMastery mastery = null;
    try {
      mastery = Zoe.getRiotApi().getChampionMasteriesBySummonerByChampionWithRateLimit(platform, summonerId, championId, forceRefreshCache);
    } catch(RiotApiException e) {
      logger.debug("Impossible to get mastery score : {}", e.getMessage());
      if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
        return "0";
      }
      return "?";
    }

    if(mastery == null) {
      return "0";
    }
    
    StringBuilder masteryString = new StringBuilder();

    long points = mastery.getChampionPoints();
    if(points > 1000 && points < 1000000) {
      masteryString.append(points / 1000 + "K");
    } else if(points > 1000000) {
      masteryString.append(points / 1000000 + "M");
    } else {
      masteryString.append(Long.toString(points));
    }

    try {
      Mastery masteryLevel = Mastery.getEnum(mastery.getChampionLevel());
      masteryString.append(Ressources.getMasteryEmote().get(masteryLevel).getEmote().getAsMention());
    } catch(NullPointerException | IllegalArgumentException e) {
      masteryString.append("");
    }

    return masteryString.toString();
  }

  public static String getActualGameStatus(CurrentGameInfo currentGameInfo, String language) {

    if(currentGameInfo == null) {
      return LanguageManager.getText(language, "informationPanelNotInGame");
    }

    String gameStatus = LanguageManager.getText(language,
        NameConversion.convertGameQueueIdToString(currentGameInfo.getGameQueueConfigId())) + " ";

    double minutesOfGames = 0.0;

    if(currentGameInfo.getGameLength() != 0l) {
      minutesOfGames = currentGameInfo.getGameLength() + 180.0;
    }

    minutesOfGames = minutesOfGames / 60.0;
    String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
    int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
    int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

    gameStatus += "(" + minutesGameLength + "m " + secondesGameLength + "s)";

    return gameStatus;
  }
}
