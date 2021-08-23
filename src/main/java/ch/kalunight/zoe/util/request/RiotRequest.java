package ch.kalunight.zoe.util.request;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.KDAReceiver;
import ch.kalunight.zoe.model.MatchReceiver;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.player_data.Rank;
import ch.kalunight.zoe.model.player_data.Tier;
import ch.kalunight.zoe.service.match.MatchCollectorReciverWorker;
import ch.kalunight.zoe.service.match.MatchReceiverWorker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.LanguageUtil;
import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.basic.exceptions.APIHTTPErrorReason;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.impl.lol.builders.championmastery.ChampionMasteryBuilder;
import no.stelar7.api.r4j.impl.lol.builders.matchv5.match.MatchBuilder;
import no.stelar7.api.r4j.impl.lol.builders.summoner.SummonerBuilder;
import no.stelar7.api.r4j.pojo.lol.championmastery.ChampionMastery;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class RiotRequest {

  private static final Logger logger = LoggerFactory.getLogger(RiotRequest.class);

  private static final DecimalFormat df = new DecimalFormat("###.#");

  private RiotRequest() {}

  public static FullTier getSoloqRank(String summonerId, LeagueShard region) {

    List<LeagueEntry> listLeague;
    try {
      listLeague = Zoe.getRiotApi().getLoLAPI().getLeagueAPI().getLeagueEntries(region, summonerId);
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

  public static FullTier getFlexRank(String summonerId, LeagueShard region) {

    List<LeagueEntry> listLeague;
    try {
      listLeague = Zoe.getRiotApi().getLoLAPI().getLeagueAPI().getLeagueEntries(region, summonerId);
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

  public static String getWinrateLastMonthWithGivenChampion(String summonerId, LeagueShard region,
      int championKey, String language) {

    Summoner summoner;
    try {
      summoner = new SummonerBuilder().withPlatform(region).withSummonerId(summonerId).get();
    } catch(APIResponseException e) {
      logger.warn("Impossible to get the summoner : {}", e.getMessage());
      return LanguageManager.getText(language, "unknown");
    }

    List<LOLMatch> matchList;
    try {
      List<Integer> championsToFilter = new ArrayList<>();
      championsToFilter.add(championKey);
      matchList = getMatchHistoryOfLastMonth(region, summoner, new ArrayList<>(), championsToFilter);
    } catch(APIResponseException e) {
      logger.info("Can't acces to history : {}", e.getMessage());
      return LanguageManager.getText(language, "unknown");
    }

    if(matchList.isEmpty()) {
      return LanguageManager.getText(language, "firstGame");
    }

    int nbrWins = 0;
    int nbrGames = 0;

    for(LOLMatch match : matchList) {
      for(MatchParticipant participant : match.getParticipants()) {
        if(participant.getSummonerId().equals(summonerId)) {
          nbrGames++;

          if(participant.didWin()) {
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
  
  public static KDAReceiver getKDALastMonth(String summonerId, LeagueShard region, List<Integer> championsId) {

    KDAReceiver kdaReceiver = new KDAReceiver();
    
    try {
      Summoner summoner = new SummonerBuilder().withSummonerId(summonerId).withPlatform(region).get();

      List<LOLMatch> matchsLastMonth = getMatchHistoryOfLastMonth(region, summoner, new ArrayList<>(), championsId);

      for(LOLMatch match : matchsLastMonth) {
        for(MatchParticipant participant : match.getParticipants()) {
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

  public static List<LOLMatch> getMatchHistoryOfLastMonth(LeagueShard region, Summoner summoner,
      List<Integer> queuesList, List<Integer> championsToFilter) {
    final List<LOLMatch> matchsToReturn = new ArrayList<>();

    for(Integer queueId : queuesList) {
      LocalDateTime beginTimeToHit = LocalDateTime.now().minusMonths(1);
      Timestamp timeStampToHit = Timestamp.valueOf(beginTimeToHit);

      MatchReceiver matchReceiver = new MatchReceiver(summoner, championsToFilter, queuesList, timeStampToHit);

      boolean allMatchReceived = false;

      int startIndex = -100;

      Optional<GameQueueType> queueType = GameQueueType.getFromId(queueId);

      if(!queueType.isPresent()) {
        continue;
      }

      do {
        startIndex += 100;
        List<String> matchList = null;

        try {
          matchList = summoner.getLeagueGamesV5().withQueue(queueType.get()).withBeginIndex(startIndex).get();
          if(matchList != null) {
            List<MatchBuilder> buildersToWait = new ArrayList<>();
            for(String matchToCheck : matchList) {
              MatchBuilder matchBuilder = new MatchBuilder(region, matchToCheck);
              buildersToWait.add(matchBuilder);

              MatchCollectorReciverWorker matchWorker = new MatchCollectorReciverWorker(matchReceiver, matchBuilder, region, summoner);
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

  public static String getMasterysScore(String summonerId, int championId, LeagueShard platform) {
    ChampionMastery mastery = null;
    try {
      mastery = new ChampionMasteryBuilder().withPlatform(platform).withSummonerId(summonerId).withChampionId(championId).getChampionMastery();

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
