package ch.kalunight.zoe.riotapi;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.constant.Platform;

public class CacheManager {

  public static final File CACHE_FOLDER = new File("ressources/cache");

  private static final Gson gson = new Gson();

  private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

  private CacheManager() {
    //hide default public constructor
  }
  
  public static void setupCache() {
    if(!CachedRiotApi.CACHE_ENABLE) {
      logger.info("The cache is disable, no file will be cached.");
      return;
    }
    cleanMatchCache();
  }

  public static void cleanMatchCache() {
    if(!CachedRiotApi.CACHE_ENABLE) {
      logger.info("The cache is disable, no file will be cleaned.");
      return;
    }
    
    //TODO: clean cache
  }



  public static Match getMatch(Platform platform, String id) {
    if(!CachedRiotApi.CACHE_ENABLE) {
      return null;
    }
    
    
  }

}
