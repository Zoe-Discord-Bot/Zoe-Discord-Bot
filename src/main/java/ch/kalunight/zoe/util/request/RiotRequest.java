package ch.kalunight.zoe.util.request;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.KDAReceiver;
import ch.kalunight.zoe.model.MatchReceiver;
import ch.kalunight.zoe.model.MatchReceiverCondition;
import ch.kalunight.zoe.model.OldestGameChecker;
import ch.kalunight.zoe.model.WinRateReceiver;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.player_data.Rank;
import ch.kalunight.zoe.model.player_data.Tier;
import ch.kalunight.zoe.service.match.MatchCollectorReciverWorker;
import ch.kalunight.zoe.service.match.MatchKDAReceiverWorker;
import ch.kalunight.zoe.service.match.MatchReceiverWorker;
import ch.kalunight.zoe.service.match.MatchWinrateReceiverWorker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.LanguageUtil;
import ch.kalunight.zoe.util.NameConversion;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.constant.Platform;

public class RiotRequest {

  private static final Logger logger = LoggerFactory.getLogger(RiotRequest.class);

  private static final DecimalFormat df = new DecimalFormat("###.#");
  
  private static final int GAME_LOADED_PER_CYCLE = 50;

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

    SummonerCache summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerWithRateLimit(region, summonerId, forceRefreshCache);
    } catch(RiotApiException e) {
      logger.warn("Impossible to get the summoner : {}", e.getMessage());
      return LanguageManager.getText(language, "unknown");
    }

    OldestGameChecker gameChecker = new OldestGameChecker();
    WinRateReceiver winRateReceiver = new WinRateReceiver();
    int baseIndex = -GAME_LOADED_PER_CYCLE;

    do {
      baseIndex += GAME_LOADED_PER_CYCLE;
      
      List<String> referencesMatchList;
      try {
        referencesMatchList = Zoe.getRiotApi().getMatchListByPuuidWithRateLimit(region, summoner.getSumCacheData().getPuuid(), baseIndex, GAME_LOADED_PER_CYCLE);
      } catch(RiotApiException e) {
        logger.info("Can't acces to history : {}", e.getMessage());
        return LanguageManager.getText(language, "unknown");
      }

      if(referencesMatchList.isEmpty()) {
        return LanguageManager.getText(language, "firstGame");
      }

      MatchReceiverCondition matchCondition = new MatchReceiverCondition(championKey, summoner.getSumCacheData().getPuuid(), LocalDateTime.now(), LocalDateTime.now().minusMonths(1));
      
      for(String gameId : referencesMatchList) {
        MatchWinrateReceiverWorker matchWorker = new MatchWinrateReceiverWorker(winRateReceiver, gameId, region, summoner, matchCondition, gameChecker);
        ServerThreadsManager.getMatchsWorker(region).execute(matchWorker);
      }

      MatchReceiverWorker.awaitAll(referencesMatchList);

    }while(gameChecker.getOldestGameDateTime().isAfter(LocalDateTime.now().minusMonths(1)));

    int nbrWins = winRateReceiver.win.intValue();
    int nbrGames = nbrWins + winRateReceiver.loose.intValue();

    if(nbrGames == 0) {
      return LanguageManager.getText(language, "firstGame");
    } else if(nbrWins == 0) {
      return "0% (" + nbrWins + "W/" + (nbrGames - nbrWins) + "L)";
    }

    return df.format((nbrWins / (double) nbrGames) * 100) + "% (" + nbrWins + "W/" + (nbrGames - nbrWins) + "L)";
  }

  public static KDAReceiver getKDALastMonthOneChampionOnly(String summonerId, Platform region, int championId, boolean forceRefreshCache) {

    SummonerCache summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerWithRateLimit(region, summonerId, forceRefreshCache);
    } catch(RiotApiException e) {
      logger.warn("Impossible to get the summoner : {}", e.getMessage());
      return null;
    }
    
    OldestGameChecker gameChecker = new OldestGameChecker();
    KDAReceiver kdaReceiver = new KDAReceiver();
    int baseIndex = -GAME_LOADED_PER_CYCLE;
    
    do {
      baseIndex += GAME_LOADED_PER_CYCLE;
      
      List<String> referencesMatchList;
      try {
        referencesMatchList = Zoe.getRiotApi().getMatchListByPuuidWithRateLimit(region, summoner.getSumCacheData().getPuuid(), baseIndex, GAME_LOADED_PER_CYCLE);
      } catch(RiotApiException e) {
        logger.info("Can't acces to history : {}", e.getMessage());
        return null;
      }

      if(referencesMatchList.isEmpty()) {
        return null;
      }

      MatchReceiverCondition matchCondition = new MatchReceiverCondition(championId, summoner.getSumCacheData().getPuuid(), LocalDateTime.now(), LocalDateTime.now().minusMonths(1));
      
      for(String gameId : referencesMatchList) {
        MatchKDAReceiverWorker matchWorker = new MatchKDAReceiverWorker(kdaReceiver, gameId, region, summoner, matchCondition, gameChecker);
        ServerThreadsManager.getMatchsWorker(region).execute(matchWorker);
      }

      MatchReceiverWorker.awaitAll(referencesMatchList);

    }while(gameChecker.getOldestGameDateTime().isAfter(LocalDateTime.now().minusMonths(1)));

    return kdaReceiver;
  }

  public static KDAReceiver getKDALastMonth(String summonerId, Platform region, boolean forceRefreshCache) {

    SummonerCache summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerWithRateLimit(region, summonerId, forceRefreshCache);
    } catch(RiotApiException e) {
      logger.warn("Impossible to get the summoner : {}", e.getMessage());
      return null;
    }

    OldestGameChecker gameChecker = new OldestGameChecker();
    KDAReceiver kdaReceiver = new KDAReceiver();
    int baseIndex = -GAME_LOADED_PER_CYCLE;
    
    do {
      baseIndex += GAME_LOADED_PER_CYCLE;
      
      List<String> referencesMatchList;
      try {
        referencesMatchList = Zoe.getRiotApi().getMatchListByPuuidWithRateLimit(region, summoner.getSumCacheData().getPuuid(), baseIndex, GAME_LOADED_PER_CYCLE);
      } catch(RiotApiException e) {
        logger.info("Can't acces to history : {}", e.getMessage());
        return null;
      }

      if(referencesMatchList.isEmpty()) {
        return null;
      }

      MatchReceiverCondition matchCondition = new MatchReceiverCondition(null, summoner.getSumCacheData().getPuuid(), LocalDateTime.now(), LocalDateTime.now().minusMonths(1));
      
      for(String gameId : referencesMatchList) {
        MatchKDAReceiverWorker matchWorker = new MatchKDAReceiverWorker(kdaReceiver, gameId, region, summoner, matchCondition, gameChecker);
        ServerThreadsManager.getMatchsWorker(region).execute(matchWorker);
      }

      MatchReceiverWorker.awaitAll(referencesMatchList);

    }while(gameChecker.getOldestGameDateTime().isAfter(LocalDateTime.now().minusMonths(1)));
    
    return kdaReceiver;
  }

  public static MatchReceiver getAllMatchsByQueue(String summonerId, Platform region, boolean forceRefreshCache, Set<Integer> queuesId) {
    SummonerCache summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerWithRateLimit(region, summonerId, forceRefreshCache);
    } catch(RiotApiException e) {
      logger.warn("Impossible to get the summoner : {}", e.getMessage());
      return null;
    }

    OldestGameChecker gameChecker = new OldestGameChecker();
    MatchReceiver matchReceiver = new MatchReceiver();
    int baseIndex = -GAME_LOADED_PER_CYCLE;
    
    do {
      baseIndex += GAME_LOADED_PER_CYCLE;
      
      List<String> referencesMatchList;
      try {
        referencesMatchList = Zoe.getRiotApi().getMatchListByPuuidWithRateLimit(region, summoner.getSumCacheData().getPuuid(), baseIndex, GAME_LOADED_PER_CYCLE);
      } catch(RiotApiException e) {
        logger.info("Can't acces to history : {}", e.getMessage());
        return null;
      }

      if(referencesMatchList.isEmpty()) {
        return null;
      }

      MatchReceiverCondition matchCondition = new MatchReceiverCondition(null, summoner.getSumCacheData().getPuuid(), LocalDateTime.now(), LocalDateTime.now().minusMonths(1));
      
      for(String gameId : referencesMatchList) {
        MatchCollectorReciverWorker matchWorker = new MatchCollectorReciverWorker(matchReceiver, gameId, region, summoner, matchCondition, gameChecker);
        ServerThreadsManager.getMatchsWorker(region).execute(matchWorker);
      }

      MatchReceiverWorker.awaitAll(referencesMatchList);

    }while(gameChecker.getOldestGameDateTime().isAfter(LocalDateTime.now().minusMonths(1)));
    
    return matchReceiver;
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

    return LanguageUtil.convertMasteryToReadableText(mastery);
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
