package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.repositories.LastRankRepository;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;

public class LastRankUtil {
  
  private static Logger logger = LoggerFactory.getLogger(LastRankUtil.class);

  private LastRankUtil() {
    // hide default public constructor
  }

  /*
   * @return true if the rank have changed.
   */
  public static boolean updateTFTLastRank(LastRank lastRank, LeagueEntry tftLeagueEntry)
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
  public static boolean updateTFTLastRank(LastRank lastRank, List<LeagueEntry> tftLeagueEntries) throws SQLException {
    LeagueEntry tftLeagueEntry = null;

    for(LeagueEntry checkLeagueEntry : tftLeagueEntries) {
      if(GameQueueConfigId.RANKED_TFT.equals(GameQueueConfigId.getGameQueueIdWithQueueType(checkLeagueEntry.getQueueType()))) {
        tftLeagueEntry = checkLeagueEntry;
      }
    }

    return updateTFTLastRank(lastRank, tftLeagueEntry);
  }

  /**
   * @return return queues updated
   */
  public static List<GameQueueConfigId> updateLoLLastRank(LastRank lastRank, List<LeagueEntry> leagueEntries) throws SQLException {

    List<GameQueueConfigId> updatedRank = new ArrayList<>();
    for(LeagueEntry checkLeagueEntry : leagueEntries) {
      if(GameQueueConfigId.getGameQueueIdWithQueueType(checkLeagueEntry.getQueueType()).equals(GameQueueConfigId.SOLOQ)) {
        if(lastRank.getLastRankSoloq() == null) {
          updateSoloQRankWithoutOldData(lastRank, checkLeagueEntry);
        }else {
          updateSoloQRank(lastRank, checkLeagueEntry);
        }
        updatedRank.add(GameQueueConfigId.SOLOQ);
      }else if(GameQueueConfigId.getGameQueueIdWithQueueType(checkLeagueEntry.getQueueType()).equals(GameQueueConfigId.FLEX)) {
        if(lastRank.getLastRankFlex() == null) {
          updateFlexRankWithoutOldData(lastRank, checkLeagueEntry);
        }else {
          updateFlexRank(lastRank, checkLeagueEntry);
        }
        updatedRank.add(GameQueueConfigId.FLEX);
      }
    }
    return updatedRank;
  }

  /**
   * @return return queues updated
   */
  public static List<Integer> updateLoLLastRankIfRankDifference(LastRank lastRank, List<LeagueEntry> leagueEntries) throws SQLException {

    List<Integer> updatedRank = new ArrayList<>();
    for(LeagueEntry checkLeagueEntry : leagueEntries) {
      
      FullTier newRank;
      try {
        newRank = new FullTier(checkLeagueEntry);
      }catch(Exception e) {
        logger.error("Error while converting league entry to FullTier : {}", checkLeagueEntry);
        continue;
      }
      
      if(GameQueueConfigId.getGameQueueIdWithQueueType(checkLeagueEntry.getQueueType()).equals(GameQueueConfigId.SOLOQ)) {
        if(lastRank.getLastRankSoloq() == null) {
          updateSoloQRankWithoutOldData(lastRank, checkLeagueEntry);
        }else {
          FullTier mostRecentSavedRank = new FullTier(lastRank.getLastRankSoloq());
          if(!mostRecentSavedRank.equals(newRank)) {
            updateSoloQRank(lastRank, checkLeagueEntry);
          }
        }
        updatedRank.add(GameQueueConfigId.SOLOQ.getId());
      }else if(GameQueueConfigId.getGameQueueIdWithQueueType(checkLeagueEntry.getQueueType()).equals(GameQueueConfigId.FLEX)) {
        if(lastRank.getLastRankFlex() == null) {
          updateFlexRankWithoutOldData(lastRank, checkLeagueEntry);
        }else {
          FullTier mostRecentSavedRank = new FullTier(lastRank.getLastRankFlex());
          if(!mostRecentSavedRank.equals(newRank)) {
            updateFlexRank(lastRank, checkLeagueEntry);
          }
        }
        updatedRank.add(GameQueueConfigId.FLEX.getId());
      }
    }
    return updatedRank;
  }


  private static void updateSoloQRankWithoutOldData(LastRank lastRank, LeagueEntry checkLeagueEntry)
      throws SQLException {
    LastRankRepository.updateLastRankSoloqWithLeagueAccountId(checkLeagueEntry, lastRank, LocalDateTime.now());
    lastRank.setLastRankSoloq(checkLeagueEntry);
  }


  private static void updateSoloQRank(LastRank lastRank, LeagueEntry checkLeagueEntry) throws SQLException {
    LastRankRepository.updateLastRankSoloqSecondWithLeagueAccountId(lastRank.getLastRankSoloq(), lastRank, LocalDateTime.now());
    lastRank.setLastRankSoloqSecond(lastRank.getLastRankSoloq());
    updateSoloQRankWithoutOldData(lastRank, checkLeagueEntry);
  }


  private static void updateFlexRankWithoutOldData(LastRank lastRank, LeagueEntry checkLeagueEntry) throws SQLException {
    LastRankRepository.updateLastRankFlexWithLeagueAccountId(checkLeagueEntry, lastRank, LocalDateTime.now());
    lastRank.setLastRankFlex(checkLeagueEntry);
  }


  private static void updateFlexRank(LastRank lastRank, LeagueEntry checkLeagueEntry) throws SQLException {
    LastRankRepository.updateLastRankFlexSecondWithLeagueAccountId(lastRank.getLastRankFlex(), lastRank, LocalDateTime.now());
    lastRank.setLastRankFlexSecond(lastRank.getLastRankFlex());
    updateFlexRankWithoutOldData(lastRank, checkLeagueEntry);
  }

}
