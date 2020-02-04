package ch.kalunight.zoe.riotapi;

import java.io.File;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.SavedMatchCacheRepository;
import ch.kalunight.zoe.service.CleanCacheService;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.constant.Platform;

public class CacheManager {

  public static final File CACHE_FOLDER = new File("ressources/cache");

  private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

  private CacheManager() {
    //hide default public constructor
  }
  
  public static void setupCache() {
    if(!CachedRiotApi.CACHE_ENABLE) {
      logger.info("The cache is disable, no file will be cached.");
      return;
    }
    ServerData.getServerExecutor().execute(new CleanCacheService());
  }

  public static void cleanMatchCache() throws SQLException {
    if(!CachedRiotApi.CACHE_ENABLE) {
      logger.info("The cache is disable, no file will be cleaned.");
      return;
    }
    
    SavedMatchCacheRepository.cleanOldMatchCatch();
  }

  public static DTO.MatchCache getMatch(Platform platform, long gameid) throws SQLException {
    if(!CachedRiotApi.CACHE_ENABLE) {
      return null;
    }
    
   return SavedMatchCacheRepository.getMatch(gameid, platform);
  }
  
  public static void createCacheMatch(Platform platform, Match match) throws SQLException {
    if(!CachedRiotApi.CACHE_ENABLE) {
      return;
    }
    
    SavedMatchCacheRepository.createMatchCache(match.getGameId(), platform, match);
  }

}
