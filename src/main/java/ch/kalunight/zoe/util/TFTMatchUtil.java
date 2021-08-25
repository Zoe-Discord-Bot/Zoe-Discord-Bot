package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.static_data.TFTMatchWithId;
import ch.kalunight.zoe.repositories.LastRankRepository;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.pojo.tft.TFTMatch;

public class TFTMatchUtil {

  private TFTMatchUtil() {
    // Hide default public constructor
  }

  public static List<TFTMatchWithId> getTFTRankedMatchsSinceTheLastMessage(LeagueAccount leagueAccount, LastRank lastRank) throws SQLException {
    
    List<TFTMatchWithId> matchs = new ArrayList<>();

    List<String> tftMatchsList = Zoe.getRiotApi().getTFTMatchList(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_tftPuuid, 5);

    if(!tftMatchsList.isEmpty() && lastRank.lastRank_tftLastTreatedMatchId != null
        && tftMatchsList.get(0).equals(lastRank.lastRank_tftLastTreatedMatchId)) { //if last treated match is the same than the last available match
      return matchs;
    }
    
    for(String matchId : tftMatchsList) {
      
      if(lastRank.lastRank_tftLastTreatedMatchId != null && lastRank.lastRank_tftLastTreatedMatchId.equals(matchId)) {
        break;
      }

      TFTMatch match = Zoe.getRiotApi().getTFTMatch(leagueAccount.leagueAccount_server, matchId);
      
      if(match != null && match.getQueue() == GameQueueType.TEAMFIGHT_TACTICS_RANKED) {
        matchs.add(new TFTMatchWithId(matchId, match));
        
        if(lastRank.lastRank_tftLastTreatedMatchId == null) {
          LastRankRepository.updateLastRankTFTLastTreatedMatch(matchId, lastRank);
          matchs.clear();
          return matchs;
        }
      }
    }
    
    if(lastRank.lastRank_tftLastTreatedMatchId != null) {
      matchs = getMatchsAfterLastGame(matchs, lastRank.lastRank_tftLastTreatedMatchId, leagueAccount.leagueAccount_server);
    }

    if(!matchs.isEmpty()) {
      
      TFTMatchWithId lastRankedMatch = getLatestMatch(matchs);
      
      if(lastRankedMatch != null) {
         
        LastRankRepository.updateLastRankTFTLastTreatedMatch(lastRankedMatch.getMatchId(), lastRank);
      }
    }
    return matchs;
  }

  private static List<TFTMatchWithId> getMatchsAfterLastGame(List<TFTMatchWithId> matchs, String lastTreatedGameID, ZoePlatform platform) {
    
    List<TFTMatchWithId> matchsAfterTheGame = new ArrayList<>();
    
    TFTMatchWithId lastTreatedMatch = null;
    for(TFTMatchWithId match : matchs) {
      if(match.getMatchId().equals(lastTreatedGameID)) {
        lastTreatedMatch = match;
        break;
      }
    }
    
    if(lastTreatedMatch == null) {
      return matchs;
    }
    
    for(TFTMatchWithId match : matchs) {
      if(lastTreatedMatch.getMatch().getMatchCreation() < match.getMatch().getMatchCreation()) {
        matchsAfterTheGame.add(match);
      }
    }
    return matchsAfterTheGame;
  }

  private static TFTMatchWithId getLatestMatch(List<TFTMatchWithId> matchs) {
    TFTMatchWithId lastRankedMatch = null;
    
    for(TFTMatchWithId match : matchs) {
      if(lastRankedMatch == null || lastRankedMatch.getMatch().getMatchCreation() < match.getMatch().getMatchCreation()) {
        lastRankedMatch = match;
      }
    }
    return lastRankedMatch;
  }
  
}
