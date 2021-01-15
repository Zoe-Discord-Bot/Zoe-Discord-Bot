package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.repositories.LastRankRepository;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.tft_league.dto.TFTLeagueEntry;

public class LastRankUtil {
  
  private LastRankUtil() {
    // hide default public constructor
  }
  
  
  /*
   * @return true if the rank have changed.
   */
  public static boolean updateTFTLastRank(LastRank lastRank, TFTLeagueEntry tftLeagueEntry)
      throws SQLException {
  
    if(tftLeagueEntry != null) {
      FullTier fullTier = new FullTier(tftLeagueEntry);
  
      if(lastRank.getLastRankTft() == null) {
        LastRankRepository.updateLastRankTftWithLeagueAccountId(tftLeagueEntry, lastRank, LocalDateTime.now());
        lastRank.setLastRankTft(tftLeagueEntry);
      }else if (!fullTier.equals(new FullTier(lastRank.getLastRankTft()))){
        LastRankRepository.updateLastRankTftWithLeagueAccountId(tftLeagueEntry, lastRank, LocalDateTime.now());
        LastRankRepository.updateLastRankTftSecondWithLeagueAccountId(lastRank.getLastRankTft(), lastRank, LocalDateTime.now());
        lastRank.setLastRankTftSecond(lastRank.getLastRankTft());
        lastRank.setLastRankTft(tftLeagueEntry);
        return true;
      }
    }
    return false;
  }
  
  /*
   * @return true if the rank have changed.
   */
  public static boolean updateTFTLastRank(LastRank lastRank, Set<TFTLeagueEntry> tftLeagueEntries) throws SQLException {
    TFTLeagueEntry tftLeagueEntry = null;
    
    for(TFTLeagueEntry checkLeagueEntry : tftLeagueEntries) {
      if(checkLeagueEntry.getQueueType().equals(GameQueueConfigId.RANKED_TFT.getQueueType())) {
        tftLeagueEntry = checkLeagueEntry;
      }
    }
    
    return updateTFTLastRank(lastRank, tftLeagueEntry);
  }
  
  /**
   * @return return queues updated
   */
  public static List<Integer> updateLoLLastRank(LastRank lastRank, Set<LeagueEntry> leagueEntries) throws SQLException {

    List<Integer> updatedRank = new ArrayList<>();
    for(LeagueEntry checkLeagueEntry : leagueEntries) {
      if(checkLeagueEntry.getQueueType().equals(GameQueueConfigId.SOLOQ.getQueueType())) {
        if(lastRank.getLastRankSoloq() == null) {
          LastRankRepository.updateLastRankSoloqWithLeagueAccountId(checkLeagueEntry, lastRank, LocalDateTime.now());
          lastRank.setLastRankSoloq(checkLeagueEntry);
        }else {
          LastRankRepository.updateLastRankSoloqSecondWithLeagueAccountId(lastRank.getLastRankSoloq(), lastRank, LocalDateTime.now());
          lastRank.setLastRankSoloqSecond(lastRank.getLastRankSoloq());
          LastRankRepository.updateLastRankSoloqWithLeagueAccountId(checkLeagueEntry, lastRank, LocalDateTime.now());
          lastRank.setLastRankSoloq(checkLeagueEntry);
        }
        updatedRank.add(GameQueueConfigId.SOLOQ.getId());
      }else if(checkLeagueEntry.getQueueType().equals(GameQueueConfigId.FLEX.getQueueType())) {
        if(lastRank.getLastRankFlex() == null) {
          LastRankRepository.updateLastRankFlexWithLeagueAccountId(checkLeagueEntry, lastRank, LocalDateTime.now());
          lastRank.setLastRankFlex(checkLeagueEntry);
        }else {
          LastRankRepository.updateLastRankFlexSecondWithLeagueAccountId(lastRank.getLastRankFlex(), lastRank, LocalDateTime.now());
          lastRank.setLastRankFlexSecond(lastRank.getLastRankFlex());
          LastRankRepository.updateLastRankFlexWithLeagueAccountId(checkLeagueEntry, lastRank, LocalDateTime.now());
          lastRank.setLastRankFlex(checkLeagueEntry);
        }
        updatedRank.add(GameQueueConfigId.FLEX.getId());
      }
    }
    return updatedRank;
  }
  
}
