package ch.kalunight.zoe.util.request;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.player_data.Rank;
import ch.kalunight.zoe.model.player_data.Tier;
import ch.kalunight.zoe.model.static_data.Mastery;
import ch.kalunight.zoe.model.static_data.SpellingLanguage;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.Ressources;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class RiotRequest {

  private static final Logger logger = LoggerFactory.getLogger(RiotRequest.class);

  private static final DecimalFormat df = new DecimalFormat("###.#");

  private RiotRequest() {}

  public static FullTier getSoloqRank(String summonerId, Platform region, CallPriority priority) {

    Set<LeagueEntry> listLeague;
    try {
      listLeague = Zoe.getRiotApi().getLeagueEntriesBySummonerId(region, summonerId, priority);
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

  public static String getWinrateLastMonthWithGivenChampion(String summonerId, Platform region,
      int championKey, SpellingLanguage language) {

    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummoner(region, summonerId, CallPriority.NORMAL);
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

    int nbrGames = 0;
    int nbrWins = 0;

    for(MatchReference matchReference : referencesMatchList) {

      Match match;
      try {
        try {
          match = Zoe.getRiotApi().getMatch(region, matchReference.getGameId(), CallPriority.NORMAL);
        } catch(RiotApiException e) {
          logger.debug("Match ungetable from api : {}", e.getMessage());
          continue;
        }

        Participant participant = match.getParticipantByAccountId(summoner.getAccountId());

        if(participant != null && participant.getTimeline().getCreepsPerMinDeltas() != null) { // Check if the game has been canceled

          String result = match.getTeamByTeamId(participant.getTeamId()).getWin();
          if(result.equalsIgnoreCase("Win") || result.equalsIgnoreCase("Fail")) {
            nbrGames++;
          }

          if(result.equalsIgnoreCase("Win")) {
            nbrWins++;
          }
        }
      } catch(NullPointerException e) {
        logger.warn("Error catched in match (some value are null, NullPointerException : {}", e.getMessage(), e);
      }
    }

    if(nbrGames == 0) {
      return LanguageManager.getText(language, "firstGame");
    } else if(nbrWins == 0) {
      return "0% (" + nbrWins + "W/" + (nbrGames - nbrWins) + "L)";
    }

    return df.format((nbrWins / (double) nbrGames) * 100) + "% (" + nbrWins + "W/" + (nbrGames - nbrWins) + "L)";
  }

  private static List<MatchReference> getMatchHistoryOfLastMonthWithTheGivenChampion(Platform region, int championKey, Summoner summoner)
      throws RiotApiException {
    final List<MatchReference> referencesMatchList = new ArrayList<>();

    DateTime actualTime = DateTime.now();
    DateTime beginTime = actualTime.minusWeeks(1);

    Set<Integer> championToFilter = new HashSet<>();
    championToFilter.add(championKey);

    for(int i = 0; i < 4; i++) {

      MatchList matchList = null;

      try {
        matchList = Zoe.getRiotApi().getMatchListByAccountId(region, summoner.getAccountId(), championToFilter, null, null,
            beginTime.getMillis(), actualTime.getMillis(), -1, -1, CallPriority.NORMAL);
        if(matchList.getMatches() != null) {
          referencesMatchList.addAll(matchList.getMatches());
        }
      } catch(RiotApiException e) {
        logger.debug("Impossible to get matchs history : {}", e.getMessage());
        if(e.getErrorCode() != RiotApiException.DATA_NOT_FOUND) {
          throw e;
        }
      }

      actualTime = actualTime.minusWeeks(1);
      beginTime = actualTime.minusWeeks(1);
    }
    return referencesMatchList;
  }

  public static String getMasterysScore(String summonerId, int championId, Platform platform) {
    ChampionMastery mastery = null;
    try {
      mastery = Zoe.getRiotApi().getChampionMasteriesBySummonerByChampion(platform, summonerId, championId, CallPriority.NORMAL);
    } catch(RiotApiException e) {
      logger.debug("Impossible to get mastery score : {}", e.getMessage());
      if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
        return "0";
      }
      return "?";
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

  public static String getActualGameStatus(CurrentGameInfo currentGameInfo, SpellingLanguage language) {

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
