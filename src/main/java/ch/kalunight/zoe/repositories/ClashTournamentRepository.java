package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.kalunight.zoe.model.dto.DTO;
import net.rithms.riot.api.endpoints.clash.dto.ClashTournament;
import net.rithms.riot.constant.Platform;

public class ClashTournamentRepository {
  
  private static final Gson gson = new GsonBuilder().create();
  
  private static final String SELECT_CLASH_TOURNAMENT_WITH_REGION = "SELECT " + 
      "SELECT " + 
      "clash_tournament_cache.clashtourcache_id, " + 
      "clash_tournament_cache.clashtourcache_server, " + 
      "clash_tournament_cache.clashtourcache_data, " +
      "clash_tournament_cache.clashtourcache_lastrefresh " +
      "FROM clash_tournament_cache " + 
      "WHERE clash_tournament_cache.clashtourcache_server = '%s'";
  
  private static final String UPDATE_CLASH_TOURNAMENT_CACHE_WITH_ID = 
      "UPDATE clash_tournament_cache SET clashtourcache_data = '%s', clashtourcache_lastrefresh = '%s' WHERE clashtourcache_id = %d";
  
  private static final String INSERT_CLASH_TOURNAMENT_CACHE = "INSERT INTO clash_tournament_cache (clashTourCache_server, clashTourCache_data, clashTourCache_lastRefresh) VALUES ('%s', '%s', '%s')";
  
  private ClashTournamentRepository() {
    //hide default public constructor
  }
  
  @Nullable
  public static DTO.ClashTournamentCache getClashTournamentCache(Platform platform) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_CLASH_TOURNAMENT_WITH_REGION, platform.getName());
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.ClashTournamentCache(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static void updateClashTournamentCache(List<ClashTournament> listTournament, long cacheId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_CLASH_TOURNAMENT_CACHE_WITH_ID, gson.toJson(listTournament),
          DTO.DB_TIME_PATTERN.format(LocalDateTime.now()), cacheId);
      query.execute(finalQuery);
    }
  }
  
  public static void createClashTournamentCache(Platform platform, List<ClashTournament> clashTourCacheData) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(INSERT_CLASH_TOURNAMENT_CACHE, platform.getName(), clashTourCacheData,
          DTO.DB_TIME_PATTERN.format(LocalDateTime.now()));
      
      query.execute(finalQuery);
    }
  }
  
  
}
