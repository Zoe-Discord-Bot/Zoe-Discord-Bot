package ch.kalunight.zoe.util.request;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.FullTier;
import ch.kalunight.zoe.model.Mastery;
import ch.kalunight.zoe.model.Rank;
import ch.kalunight.zoe.model.Tier;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.Ressources;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;
import net.rithms.riot.api.endpoints.league.dto.LeaguePosition;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;


public class RiotRequest {

  private static final Logger logger = LoggerFactory.getLogger(RiotRequest.class);

  private static final DecimalFormat df = new DecimalFormat("###.##");

  private static final int MAX_GAME_FOR_WINRATE = 20 - 1;

  private RiotRequest() {}

  public static FullTier getSoloqRank(String summonerId, Platform region) {

    Set<LeaguePosition> listLeague;
    try {
      listLeague = Zoe.getRiotApi().getLeaguePositionsBySummonerId(region, summonerId);
    } catch(RiotApiException e) {
      logger.warn("Error with riot api : {}", e.getMessage());
      return new FullTier(Tier.UNKNOWN, Rank.UNKNOWN, 0);
    }

    Iterator<LeaguePosition> gettableList = listLeague.iterator();

    Tier rank = Tier.UNRANKED;
    Rank tier = Rank.UNRANKED;
    int leaguePoints = 0;

    while(gettableList.hasNext()) {
      LeaguePosition leaguePosition = gettableList.next();

      if(leaguePosition.getQueueType().equals("RANKED_SOLO_5x5")) {
        rank = Tier.valueOf(leaguePosition.getTier());
        tier = Rank.valueOf(leaguePosition.getRank());
        leaguePoints = leaguePosition.getLeaguePoints();
      }
    }

    return new FullTier(rank, tier, leaguePoints);
  }
  
  public static String getWinrateLateMonthWithGivenChampion(String summonerId, Platform region, int championKey) {
    DateTime actualTime = DateTime.now();
    DateTime beginTime = actualTime.minusMonths(1);
    
    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummoner(Platform.EUW, summonerId);
    } catch(RiotApiException e) {
      logger.warn("Impossible to get the summoner : {}", e.getMessage());
      return "Any data";
    }
    
    MatchList matchList = null;
    
    Set<Integer> championToFilter = new HashSet<>();
    championToFilter.add(championKey);
    
    try {
      matchList = Zoe.getRiotApi().getMatchListByAccountId(region, summoner.getAccountId(), championToFilter, null, null,
          beginTime.getMillis(), actualTime.getMillis(), -1, -1);
    } catch(RiotApiException e) {
      logger.warn("Impossible to get matchs history : {}", e.getMessage());
      
      if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
        return "Any game";
      }else {
        return "Unknown";
      }
    }
    
    if(matchList == null) {
      return "Unknown";
    }
    
    int nbrGames = 0;
    int nbrWins = 0;
    
    for(MatchReference matchReference : matchList.getMatches()) {
      
      Match match;
      try {
        match = Zoe.getRiotApi().getMatch(region, matchReference.getGameId());
      } catch(RiotApiException e) {
        logger.warn("Match ungetable from api : {}", e.getMessage());
        continue;
      }
      
      Participant participant = match.getParticipantByAccountId(summoner.getAccountId());
      
      if(participant != null && participant.getTimeline().getCreepsPerMinDeltas() != null) {

        String result = match.getTeamByTeamId(participant.getTeamId()).getWin();
        if(result.equalsIgnoreCase("Win") || result.equalsIgnoreCase("Fail")) {
          nbrGames++;
        }

        if(result.equalsIgnoreCase("Win")) {
          nbrWins++;
        }
      }
    }
    
    if(nbrGames == 0) {
      return "First game";
    } else if(nbrWins == 0) {
      return "0% (" + nbrGames + " games)";
    }

    return df.format((nbrWins / (double) nbrGames) * 100) + "% (" + nbrGames + " games)";
  }

  public static String getWinrateLast20Games(String summonerId) {
    DateTime actualTime = DateTime.now();

    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummoner(Platform.EUW, summonerId);
    } catch(RiotApiException e) {
      logger.warn("Impossible d'obtenir le summoner : {}", e.getMessage());
      return "Aucune donnés";
    }

    List<MatchReference> matchesReferences = new ArrayList<>();

    DateTime beginTime = actualTime.minusWeeks(1);

    MatchList matchList = null;

    try {
      matchList = Zoe.getRiotApi().getMatchListByAccountId(Platform.EUW, summoner.getAccountId(), null, null, null,
          beginTime.getMillis(), actualTime.getMillis(), -1, -1);
    } catch(RiotApiException e) {
      logger.warn("Impossible d'obtenir la list de match : {}", e.getMessage());
    }

    if(matchList != null) {
      matchesReferences.addAll(matchList.getMatches());
    }

    if(matchesReferences.size() > MAX_GAME_FOR_WINRATE) {
      int size = matchesReferences.size();

      for(int i = size - 1; i > MAX_GAME_FOR_WINRATE; i--) {
        matchesReferences.remove(i);
      }
    }

    int nbrGames = 0;
    int nbrWin = 0;

    for(int i = 0; i < matchesReferences.size(); i++) {
      MatchReference matchReference = matchesReferences.get(i);

      Match match = null;
      try {
        match = Zoe.getRiotApi().getMatch(Platform.EUW, matchReference.getGameId());
      } catch(RiotApiException e) {
        logger.warn("Match ungetable from api : {}", e.getMessage());
      }

      if(match != null) {
        Participant participant = match.getParticipantByAccountId(summoner.getAccountId());

        if(participant != null && participant.getTimeline().getCreepsPerMinDeltas() != null) {

          String result = match.getTeamByTeamId(participant.getTeamId()).getWin();
          if(result.equalsIgnoreCase("Win") || result.equalsIgnoreCase("Fail")) {
            nbrGames++;
          }

          if(result.equalsIgnoreCase("Win")) {
            nbrWin++;
          }
        }
      }
    }

    if(nbrGames == 0) {
      return "Première game de ce mois";
    } else if(nbrWin == 0) {
      return "0% (" + nbrGames + " parties)";
    }

    return df.format((nbrWin / (double) nbrGames) * 100) + "% (" + nbrGames + " parties)";
  }

  public static String getMasterysScore(String summonerId, int championId) {
    ChampionMastery mastery = null;
    try {
      mastery = Zoe.getRiotApi().getChampionMasteriesBySummonerByChampion(Platform.EUW, summonerId, championId);
    } catch(RiotApiException e) {
      logger.warn("Impossible d'obtenir le score de mastery : {}", e.getMessage());
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
    }catch(NullPointerException | IllegalArgumentException e) {
      masteryString.append("");
    }
    
    return masteryString.toString();
  }

  public static String getActualGameStatus(CurrentGameInfo currentGameInfo) {

    if(currentGameInfo == null) {
      return "Pas en game";
    }

    String gameStatus = NameConversion.convertGameQueueIdToString(currentGameInfo.getGameQueueConfigId()) + " ";

    double minutesOfGames = (currentGameInfo.getGameLength() + 180.0) / 60.0;
    String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
    int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
    int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

    gameStatus += "(" + minutesGameLength + "m " + secondesGameLength + "s)";

    return gameStatus;
  }
}
