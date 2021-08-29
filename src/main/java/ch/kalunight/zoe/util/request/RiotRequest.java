package ch.kalunight.zoe.util.request;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.KDAReceiver;
import ch.kalunight.zoe.model.MatchReceiver;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;
import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.player_data.Rank;
import ch.kalunight.zoe.model.player_data.Tier;
import ch.kalunight.zoe.riotapi.MatchKeyString;
import ch.kalunight.zoe.service.match.MatchCollectorReciverWorker;
import ch.kalunight.zoe.service.match.MatchReceiverWorker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.LanguageUtil;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.basic.exceptions.APIHTTPErrorReason;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;

public class RiotRequest {

  private static final Logger logger = LoggerFactory.getLogger(RiotRequest.class);

  private static final DecimalFormat df = new DecimalFormat("###.#");

  private RiotRequest() {}

  public static FullTier getSoloqRank(String summonerId, ZoePlatform region) {

    List<LeagueEntry> listLeague;
    try {
      listLeague = Zoe.getRiotApi().getLeagueEntryBySummonerId(region, summonerId);
    } catch(APIResponseException e) {
      logger.info("Error with riot api : {}", e.getMessage());
      return new FullTier(Tier.UNKNOWN, Rank.UNKNOWN, 0);
    }

    Tier rank = Tier.UNRANKED;
    Rank tier = Rank.UNRANKED;
    int leaguePoints = 0;

    for(LeagueEntry leaguePosition : listLeague) {
      if(leaguePosition.getQueueType().equals(GameQueueType.RANKED_SOLO_5X5)) {
        rank = Tier.valueOf(leaguePosition.getTier());
        tier = Rank.valueOf(leaguePosition.getRank());
        leaguePoints = leaguePosition.getLeaguePoints();
      }
    }

    return new FullTier(rank, tier, leaguePoints);
  }

  public static FullTier getFlexRank(String summonerId, ZoePlatform region) {

    List<LeagueEntry> listLeague;
    try {
      listLeague = Zoe.getRiotApi().getLeagueEntryBySummonerId(region, summonerId);
    } catch(APIResponseException e) {
      logger.info("Error with riot api : {}", e.getMessage());
      return new FullTier(Tier.UNKNOWN, Rank.UNKNOWN, 0);
    }

    Tier rank = Tier.UNRANKED;
    Rank tier = Rank.UNRANKED;
    int leaguePoints = 0;

    for(LeagueEntry leaguePosition : listLeague) {
      if(leaguePosition.getQueueType().equals(GameQueueType.RANKED_FLEX_SR)) {
        rank = Tier.valueOf(leaguePosition.getTier());
        tier = Rank.valueOf(leaguePosition.getRank());
        leaguePoints = leaguePosition.getLeaguePoints();
      }
    }

    return new FullTier(rank, tier, leaguePoints);
  }

  public static String getWinrateLastMonthWithGivenChampion(String summonerId, ZoePlatform region,
      int championKey, List<GameQueueType> queuesList, String language) {

    SavedSummoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerBySummonerId(region, summonerId);
    } catch(APIResponseException e) {
      logger.warn("Impossible to get the summoner : {}", e.getMessage());
      return LanguageManager.getText(language, "unknown");
    }

    List<SavedMatch> matchList;
    try {
      List<Integer> championsToFilter = new ArrayList<>();
      championsToFilter.add(championKey);
      matchList = getMatchHistoryOfLastMonth(region, summoner, queuesList, championsToFilter);
    } catch(APIResponseException e) {
      logger.info("Can't acces to history : {}", e.getMessage());
      return LanguageManager.getText(language, "unknown");
    }

    if(matchList.isEmpty()) {
      return LanguageManager.getText(language, "firstGame");
    }

    int nbrWins = 0;
    int nbrGames = 0;

    for(SavedMatch match : matchList) {
      for(SavedMatchPlayer participant : match.getPlayers()) {
        if(participant.getSummonerId().equals(summonerId)) {
          nbrGames++;

          if(participant.didWin(match)) {
            nbrWins++;
          }
          break;
        }
      }
    }

    if(nbrGames == 0) {
      return LanguageManager.getText(language, "firstGame");
    } else if(nbrWins == 0) {
      return "0% (" + nbrWins + "W/" + (nbrGames - nbrWins) + "L)";
    }

    return df.format((nbrWins / (double) nbrGames) * 100) + "% (" + nbrWins + "W/" + (nbrGames - nbrWins) + "L)";
  }
  
  public static KDAReceiver getKDALastMonth(String summonerId, ZoePlatform region, List<Integer> championsId) {

    KDAReceiver kdaReceiver = new KDAReceiver();
    
    try {
      SavedSummoner summoner = Zoe.getRiotApi().getSummonerBySummonerId(region, summonerId);

      List<SavedMatch> matchsLastMonth = getMatchHistoryOfLastMonth(region, summoner, new ArrayList<>(), championsId);

      for(SavedMatch match : matchsLastMonth) {
        for(SavedMatchPlayer participant : match.getPlayers()) {
          if(participant.getSummonerId().equals(summoner.getSummonerId())) {
            kdaReceiver.kills.addAndGet(participant.getKills());
            kdaReceiver.deaths.addAndGet(participant.getDeaths());
            kdaReceiver.assists.addAndGet(participant.getAssists());
            break;
          }
        }
      }

      return kdaReceiver;
    }catch(APIResponseException e) {
      logger.error("Api response error", e);
      return kdaReceiver;
    }
  }

  public static List<SavedMatch> getMatchHistoryOfLastMonth(ZoePlatform region, SavedSummoner summoner,
      List<GameQueueType> queuesList, List<Integer> championsToFilter) {
    final List<SavedMatch> matchsToReturn = new ArrayList<>();

    for(GameQueueType queueId : queuesList) {
      LocalDateTime beginTimeToHit = LocalDateTime.now().minusMonths(1);
      Timestamp timeStampToHit = Timestamp.valueOf(beginTimeToHit);

      MatchReceiver matchReceiver = new MatchReceiver(summoner, championsToFilter, queuesList, timeStampToHit);

      boolean allMatchReceived = false;

      int startIndex = -100;

      do {
        startIndex += 100;
        List<String> matchList = null;

        try {
          matchList = Zoe.getRiotApi().getMatchListByPuuid(region, summoner.getPuuid(), queueId, startIndex);
          if(!matchList.isEmpty()) {
            List<MatchKeyString> buildersToWait = new ArrayList<>();
            for(String matchToCheck : matchList) {
              MatchKeyString matchKey = new MatchKeyString(region, matchToCheck);
              buildersToWait.add(matchKey);

              MatchCollectorReciverWorker matchWorker = new MatchCollectorReciverWorker(matchReceiver, matchKey, region, summoner);
              ServerThreadsManager.getMatchsWorker(region).execute(matchWorker);
            }

            MatchReceiverWorker.awaitAll(buildersToWait);

            if(matchReceiver.isTimestampHited()) {
              allMatchReceived = true;
            }
          }else {
            allMatchReceived = true;
          }
        } catch(APIResponseException e) {
          logger.debug("Impossible to get matchs history : {}", e.getMessage());
          if(e.getReason() != APIHTTPErrorReason.ERROR_404) {
            throw e;
          }
        }

        if(allMatchReceived) {
          matchsToReturn.addAll(matchReceiver.matchs);
        }

      }while(!allMatchReceived);
    }

    return matchsToReturn;
  }

  public static String getMasterysScore(String summonerId, int championId, ZoePlatform platform) {
    SavedSimpleMastery mastery = null;
    try {
      List<SavedSimpleMastery> allMasteries = Zoe.getRiotApi().getChampionMasteryBySummonerId(platform, summonerId);

      for(SavedSimpleMastery masteryToCheck : allMasteries) {
        if(masteryToCheck.getChampionId() == championId) {
          mastery = masteryToCheck;
          break;
        }
      }
      
    } catch(APIResponseException e) {
      logger.debug("Impossible to get mastery score : {}", e.getMessage());
      if(e.getReason() == APIHTTPErrorReason.ERROR_404) {
        return "0";
      }
      return "?";
    }

    if(mastery == null) {
      return "0";
    }

    return LanguageUtil.convertMasteryToReadableText(mastery);
  }
}
