package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedMatch;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.constant.Platform;

public class SavedMatchCacheRepository {
  
  private static final String SELECT_MATCH_WITH_GAMEID_AND_SERVER = "SELECT " + 
      "match_cache.mcatch_id, " + 
      "match_cache.mcatch_gameid, " + 
      "match_cache.mcatch_platform, " + 
      "match_cache.mcatch_savedmatch, " + 
      "match_cache.mcatch_creationtime " + 
      "FROM match_cache " + 
      "WHERE match_cache.mcatch_gameid = %d " + 
      "AND match_cache.mcatch_platform = '%s'";
  
  private static final String INSERT_MATCH_CATCH = "INSERT INTO match_cache "
      + "(mCatch_gameId, mCatch_platform, mCatch_savedMatch, mCatch_creationTime) VALUES (%d, '%s', '%s', '%s')";
  
  private static final String DELETE_MATCH_CACHE_OLD_OF_1_MONTHS = "DELETE FROM match_cache WHERE mcatch_creationtime > '%s'";
  
  private static final Gson gson = new GsonBuilder().create();
  
  private SavedMatchCacheRepository() {
    //hide Repo Ressources
  }
  
  public static void createMatchCache(long gameId, Platform server, Match match) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      SavedMatch savedMatch = new SavedMatch(match);
      
      LocalDateTime creationTime = Instant.ofEpochMilli(match.getGameCreation()).atZone(ZoneId.systemDefault()).toLocalDateTime();
      
      String finalQuery = String.format(INSERT_MATCH_CATCH, gameId, server.getName(), gson.toJson(savedMatch), DTO.DB_TIME_PATTERN.format(creationTime));
      query.execute(finalQuery);
    }
  }
  
  public static void cleanOldMatchCatch() throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(DELETE_MATCH_CACHE_OLD_OF_1_MONTHS, DTO.DB_TIME_PATTERN.format(LocalDateTime.now().minusMonths(1)));
      query.execute(finalQuery);
    }
  }
  
  public static DTO.MatchCache getMatch(long gameId, Platform platform) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_MATCH_WITH_GAMEID_AND_SERVER, gameId, platform.getName());
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.MatchCache(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
}
