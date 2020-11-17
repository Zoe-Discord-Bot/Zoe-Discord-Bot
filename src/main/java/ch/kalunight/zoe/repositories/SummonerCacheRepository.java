package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
      "summoner_cache.sumcache_data " + 
      "FROM summoner_cache " + 
      "WHERE summoner_cache.sumcache_summonerid = '%s' " + 
      "AND summoner_cache.sumcache_server = '%s'";
  
  private static final String INSERT_SUMMONER_CACHE = "INSERT INTO summoner_cache "
      + "(sumCache_summonerId, sumCache_server, sumCache_data) VALUES ('%s', '%s', '%s')";
  
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
  
  public static void createSummonerCache(String summonerId, Platform server, SavedSummoner summonerToCache) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(INSERT_SUMMONER_CACHE, summonerId, server, gson.toJson(summonerToCache));
      query.execute(finalQuery);
    }
  }
}
