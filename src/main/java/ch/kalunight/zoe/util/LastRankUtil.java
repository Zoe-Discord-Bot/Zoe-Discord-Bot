package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.time.LocalDateTime;
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
  
      if(lastRank.lastRank_tft == null) {
        LastRankRepository.updateLastRankTftWithLeagueAccountId(tftLeagueEntry, lastRank, LocalDateTime.now());
        lastRank.lastRank_tft = tftLeagueEntry;
      }else if (!fullTier.equals(new FullTier(lastRank.lastRank_tft))){
        LastRankRepository.updateLastRankTftWithLeagueAccountId(tftLeagueEntry, lastRank, LocalDateTime.now());
        LastRankRepository.updateLastRankTftSecondWithLeagueAccountId(lastRank.lastRank_tft, lastRank, LocalDateTime.now());
        lastRank.lastRank_tftSecond = lastRank.lastRank_tft;
        lastRank.lastRank_tft = tftLeagueEntry;
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
   * @return true if the update has been done correctly, false otherwise.
   */
  public static boolean updateLoLLastRank(LastRank lastRank, Set<LeagueEntry> leagueEntries) throws SQLException {

    for(LeagueEntry checkLeagueEntry : leagueEntries) {
      if(checkLeagueEntry.getQueueType().equals(GameQueueConfigId.SOLOQ.getQueueType())) {
        if(lastRank.lastRank_soloq == null) {
          LastRankRepository.updateLastRankSoloqWithLeagueAccountId(checkLeagueEntry, lastRank, LocalDateTime.now());
          lastRank.lastRank_soloq = checkLeagueEntry;
        }else {
          LastRankRepository.updateLastRankSoloqSecondWithLeagueAccountId(lastRank.lastRank_soloq, lastRank, LocalDateTime.now());
          lastRank.lastRank_soloqSecond = lastRank.lastRank_soloq;
          LastRankRepository.updateLastRankSoloqWithLeagueAccountId(checkLeagueEntry, lastRank, LocalDateTime.now());
          lastRank.lastRank_soloq = checkLeagueEntry;
        }
      }else if(checkLeagueEntry.getQueueType().equals(GameQueueConfigId.FLEX.getQueueType())) {
        if(lastRank.lastRank_flex == null) {
          LastRankRepository.updateLastRankFlexWithLeagueAccountId(checkLeagueEntry, lastRank, LocalDateTime.now());
          lastRank.lastRank_flex = checkLeagueEntry;
        }else {
          LastRankRepository.updateLastRankFlexSecondWithLeagueAccountId(lastRank.lastRank_flex, lastRank, LocalDateTime.now());
          lastRank.lastRank_flexSecond = lastRank.lastRank_flex;
          LastRankRepository.updateLastRankFlexWithLeagueAccountId(checkLeagueEntry, lastRank, LocalDateTime.now());
          lastRank.lastRank_flex = checkLeagueEntry;
        }
      }
    }
    return true;
  }
  
}
