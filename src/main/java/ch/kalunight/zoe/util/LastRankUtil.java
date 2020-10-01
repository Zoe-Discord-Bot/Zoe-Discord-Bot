package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.util.Set;

import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
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
  public static boolean updateTFTLastRank(LeagueAccount leagueAccount, LastRank lastRank, Set<TFTLeagueEntry> tftLeagueEntries)
      throws SQLException {
    TFTLeagueEntry tftLeagueEntry = null;

    for(TFTLeagueEntry checkLeagueEntry : tftLeagueEntries) {
      if(checkLeagueEntry.getQueueType().equals(GameQueueConfigId.RANKED_TFT.getQueueType())) {
        tftLeagueEntry = checkLeagueEntry;
      }
    }

    if(tftLeagueEntry != null) {
      FullTier fullTier = new FullTier(tftLeagueEntry);

      if(lastRank.lastRank_tft == null) {
        LastRankRepository.updateLastRankTftWithLeagueAccountId(tftLeagueEntry, leagueAccount.leagueAccount_id);
        lastRank.lastRank_tft = tftLeagueEntry;
      }else if (!fullTier.equals(new FullTier(lastRank.lastRank_tft))){
        LastRankRepository.updateLastRankTftWithLeagueAccountId(tftLeagueEntry, leagueAccount.leagueAccount_id);
        LastRankRepository.updateLastRankTftSecondWithLeagueAccountId(lastRank.lastRank_tft, leagueAccount.leagueAccount_id);
        lastRank.lastRank_tftSecond = lastRank.lastRank_tft;
        lastRank.lastRank_tft = tftLeagueEntry;
        return true;
      }
    }
    return false;
  }
  
  /**
   * @return true is the update has been done correctly, false otherwise.
   */
  public static boolean updateLoLLastRank(LeagueAccount leagueAccount, LastRank lastRank, Set<LeagueEntry> leagueEntries) throws SQLException {

    for(LeagueEntry checkLeagueEntry : leagueEntries) {
      if(checkLeagueEntry.getQueueType().equals(GameQueueConfigId.SOLOQ.getQueueType())) {
        if(lastRank.lastRank_soloq == null) {
          LastRankRepository.updateLastRankSoloqWithLeagueAccountId(checkLeagueEntry, leagueAccount.leagueAccount_id);
          lastRank.lastRank_soloq = checkLeagueEntry;
        }else {
          LastRankRepository.updateLastRankSoloqSecondWithLeagueAccountId(lastRank.lastRank_soloq, leagueAccount.leagueAccount_id);
          lastRank.lastRank_soloqSecond = lastRank.lastRank_soloq;
          LastRankRepository.updateLastRankSoloqWithLeagueAccountId(checkLeagueEntry, leagueAccount.leagueAccount_id);
          lastRank.lastRank_soloq = checkLeagueEntry;
        }
      }else if(checkLeagueEntry.getQueueType().equals(GameQueueConfigId.FLEX.getQueueType())) {
        if(lastRank.lastRank_soloq == null) {
          LastRankRepository.updateLastRankFlexWithLeagueAccountId(checkLeagueEntry, leagueAccount.leagueAccount_id);
          lastRank.lastRank_flex = checkLeagueEntry;
        }else {
          LastRankRepository.updateLastRankFlexSecondWithLeagueAccountId(lastRank.lastRank_soloq, leagueAccount.leagueAccount_id);
          lastRank.lastRank_flexSecond = lastRank.lastRank_flex;
          LastRankRepository.updateLastRankFlexWithLeagueAccountId(checkLeagueEntry, leagueAccount.leagueAccount_id);
          lastRank.lastRank_flex = checkLeagueEntry;
        }
      }
    }
    return true;
  }
  
}
