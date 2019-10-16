package ch.kalunight.zoe.riotapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.constant.Platform;

public class CacheManager {

  public static final File CACHE_FOLDER = new File("ressources/cache");

  private static final Gson gson = new Gson();

  private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

  public static void setupCache() {
    if(!CachedRiotApi.CACHE_ENABLE) {
      logger.info("The cache is disable, no file will be cached.");
      return;
    }
    
    
    if(!CACHE_FOLDER.exists()) {
      CACHE_FOLDER.mkdir();
    }

    for(Platform platform : Platform.values()) {
      File platfomCacheFolder = new File(CACHE_FOLDER.getAbsolutePath() + "/" + platform.getName());
      if(!platfomCacheFolder.exists()) {
        platfomCacheFolder.mkdir();
      }

      for(DataType dataType : DataType.values()) {
        File dataTypeCacheFolder = new File(platfomCacheFolder.getAbsolutePath() + "/" + dataType.toString());
        if(!dataTypeCacheFolder.exists()) {
          dataTypeCacheFolder.mkdir();
        }
      }
    }
  }

  public static void cleanMatchCache() {
    if(!CachedRiotApi.CACHE_ENABLE) {
      logger.info("The cache is disable, no file will be cleaned.");
      return;
    }
    
    int totalDeletedFile = 0;
    for(Platform platform : Platform.values()) {
      File matchFolder = new File(CACHE_FOLDER.getAbsoluteFile() + "/" + platform.getName() + "/" + DataType.MATCH.toString());

      for(File fileMatch : matchFolder.listFiles()) {
        if(fileMatch.getName().endsWith(".json")) {
          try(final BufferedReader reader = new BufferedReader(new FileReader(fileMatch));) {
            Match match = gson.fromJson(reader, Match.class);

            LocalDateTime creationMatch = LocalDateTime.ofInstant(Instant.ofEpochMilli(match.getGameCreation()), ZoneOffset.UTC);

            if(LocalDateTime.now().minusDays(15).isAfter(creationMatch)) {
              if(fileMatch.delete()) {
                totalDeletedFile++;
              }
            }

          } catch(IOException e) {
            logger.warn("Error in loading a file of caching system ! File Path: {}", fileMatch.getAbsoluteFile());
          } catch(JsonSyntaxException e) {
            logger.info("A file is malformed, this file will be deleted. Error : {}", e.getMessage());
            if(fileMatch.delete()) {
              totalDeletedFile++;
            }
          }
        }
      }
    }
    logger.info("Number of files deleted : {}", totalDeletedFile);
  }

  private static File getCacheFile(Platform platform, DataType dataType, String fileName) {
    return new File(CACHE_FOLDER.getAbsolutePath() + "/" + platform.getName() + "/" + dataType.toString() + "/" + fileName);
  }

  public static void putMatch(Platform platform, Match match) {
    if(!CachedRiotApi.CACHE_ENABLE) {
      return;
    }
    File file = getCacheFile(platform, DataType.MATCH, Long.toString(match.getGameId()) + ".json");

    String jsonMatch = gson.toJson(match);

    try(PrintWriter writer = new PrintWriter(file, "UTF-8");) {
      writer.write(jsonMatch);
    } catch(FileNotFoundException | UnsupportedEncodingException e) {
      logger.warn("Error in caching a match ! Platform : {}, ID : {}", platform.getName(), match.getGameId());
    }
  }

  public static Match getMatch(Platform platform, String id) {
    if(!CachedRiotApi.CACHE_ENABLE) {
      return null;
    }
    
    File file = getCacheFile(platform, DataType.MATCH, id + ".json");

    if(!file.exists()) return null;

    try(final BufferedReader reader = new BufferedReader(new FileReader(file));) {
      return gson.fromJson(reader, Match.class);

    } catch(IOException e) {
      logger.warn("Error in loading a file of caching system ! Platform : {}, ID : {}", platform.getName(), id);
    } catch(Exception e) {
      logger.warn("Unexpected error in the loading of the match. Error : ", e);
    }
    return null;
  }

}
