package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import net.rithms.riot.constant.Platform;

public class SummonerCacheRepository {

  private static final Gson gson = new GsonBuilder().create();
  
  private static final String SELECT_SUMMONER_CACHE_WITH_SUMMONER_ID_AND_SERVER = "SELECT " + 
      "summoner_cache.sumcache_id, " + 
      "summoner_cache.sumcache_summonerid, " + 
      "summoner_cache.sumcache_server, " + 
      "summoner_cache.sumcache_data, " +
      "summoner_cache.sumcache_lastrefresh " +
      "FROM summoner_cache " + 
      "WHERE summoner_cache.sumcache_summonerid = '%s' " + 
      "AND summoner_cache.sumcache_server = '%s'";
  
  private static final String INSERT_SUMMONER_CACHE = "INSERT INTO summoner_cache "
      + "(sumCache_summonerId, sumCache_server, sumCache_data, sumcache_lastrefresh) VALUES ('%s', '%s', '%s', '%s')";
  
  private static final String UPDATE_SUMMONER_CACHE_WITH_ID = 
      "UPDATE summoner_cache SET sumcache_data = '%s', sumcache_lastrefresh = '%s' WHERE sumcache_id = %d";
  
  private static final String DELETE_SUMMONER_CACHE_WITH_ID = 
      "DELETE FROM summoner_cache WHERE sumcache_id = %d";
  
  private static final String DELETE_SUMMONER_CACHE_WITH_SERVER_AND_SUMMONER_ID = 
      "DELETE FROM summoner_cache WHERE sumcache_server = '%s' AND sumcache_summonerid = '%s'";
  
  private SummonerCacheRepository() {
    //hide default public constructor
  }
  
  public static DTO.SummonerCache getSummonerWithSummonerId(String summonerId, Platform platform) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_SUMMONER_CACHE_WITH_SUMMONER_ID_AND_SERVER, summonerId, platform.getName());
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.SummonerCache(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static void updateSummonerCache(SavedSummoner savedSummoner, long cacheId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_SUMMONER_CACHE_WITH_ID, gson.toJson(savedSummoner),
          DTO.DB_TIME_PATTERN.format(LocalDateTime.now()), cacheId);
      query.execute(finalQuery);
    }
  }
  
  public static void createSummonerCache(String summonerId, Platform server, SavedSummoner summonerToCache) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(INSERT_SUMMONER_CACHE, summonerId, server.getName(), gson.toJson(summonerToCache), DTO.DB_TIME_PATTERN.format(LocalDateTime.now()));
      query.execute(finalQuery);
    }
  }
  
  public static void deleteSummonerCacheWithId(long summonerCacheId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(DELETE_SUMMONER_CACHE_WITH_ID, summonerCacheId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static void deleteSummonerCacheWithSummonerIDAndServer(Platform platform, String summonerId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(DELETE_SUMMONER_CACHE_WITH_SERVER_AND_SUMMONER_ID, platform.getName(), summonerId);
      query.executeUpdate(finalQuery);
    }
  }
}
