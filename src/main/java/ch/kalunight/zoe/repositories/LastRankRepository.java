package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ch.kalunight.zoe.model.dto.DTO;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;

public class LastRankRepository {

  private static final String INSERT_LAST_RANK = "INSERT INTO last_rank " +
      "(lastRank_fk_leagueAccount) VALUES (%d)";
  
  private static final String SELECT_LAST_RANK_WITH_LEAGUE_ACCOUNT_ID = "SELECT " + 
      "last_rank.lastrank_id, " + 
      "last_rank.lastrank_fk_leagueaccount, " + 
      "last_rank.lastrank_soloq, " +
      "last_rank.lastrank_soloqSecond, " + 
      "last_rank.lastrank_flex, " + 
      "last_rank.lastrank_flexSecond, " + 
      "last_rank.lastrank_tft, " + 
      "last_rank.lastrank_tftSecond " + 
      "FROM league_account " + 
      "INNER JOIN last_rank ON league_account.leagueaccount_id = last_rank.lastrank_fk_leagueaccount " + 
      "WHERE league_account.leagueaccount_id = %d";
  
  private static final String UPDATE_LAST_RANK_SOLOQ_WITH_ID = 
      "UPDATE last_rank SET lastRank_soloq = %s WHERE lastRank_id = %d";
  
  private static final String UPDATE_LAST_RANK_SOLOQ_SECOND_WITH_ID = 
      "UPDATE last_rank SET lastRank_soloqSecond = %s WHERE lastRank_id = %d";
  
  private static final String UPDATE_LAST_RANK_FLEX_WITH_ID = 
      "UPDATE last_rank SET lastRank_flex = %s WHERE lastRank_id = %d";

  private static final String UPDATE_LAST_RANK_FLEX_SECOND_WITH_ID = 
      "UPDATE last_rank SET lastRank_flexSecond = %s WHERE lastRank_id = %d";

  private static final String UPDATE_LAST_RANK_TFT_WITH_ID = 
      "UPDATE last_rank SET lastRank_tft = %s WHERE lastRank_id = %d";
  
  private static final String UPDATE_LAST_RANK_TFT_SECOND_WITH_ID = 
      "UPDATE last_rank SET lastRank_tftSecond = %s WHERE lastRank_id = %d";
  
  private static final String DELETE_LAST_RANK_WITH_ID = "DELETE FROM last_rank WHERE lastRank_id = %d";
  
  private static final Gson gson = new GsonBuilder().create();
  
  private LastRankRepository() {
    // hide default public constructor
  }
  
  public static void createLastRank(long leagueAccountId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(INSERT_LAST_RANK, leagueAccountId);
      query.execute(finalQuery);
    }
  }
  
  public static DTO.LastRank getLastRankWithLeagueAccountId(long leagueAccountId) throws SQLException{
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_LAST_RANK_WITH_LEAGUE_ACCOUNT_ID, leagueAccountId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.LastRank(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static void updateLastRankSoloqWithLeagueAccountId(LeagueEntry soloqRank, long leagueAccountId) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankSoloqJson;

      if(soloqRank == null) {
        lastRankSoloqJson = "NULL";
      }else {
        lastRankSoloqJson = gson.toJson(soloqRank);
        lastRankSoloqJson = "'" + lastRankSoloqJson + "'";
      }
      
      DTO.LastRank lastRank = getLastRankWithLeagueAccountId(leagueAccountId);

      if(lastRank != null) {
        String finalQuery = String.format(UPDATE_LAST_RANK_SOLOQ_WITH_ID, lastRankSoloqJson, lastRank.lastRank_id);
        query.execute(finalQuery);
      }
    }
  }
  
  public static void updateLastRankSoloqSecondWithLeagueAccountId(LeagueEntry soloqRank, long leagueAccountId) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankSoloqJson;

      if(soloqRank == null) {
        lastRankSoloqJson = "NULL";
      }else {
        lastRankSoloqJson = gson.toJson(soloqRank);
        lastRankSoloqJson = "'" + lastRankSoloqJson + "'";
      }
      
      DTO.LastRank lastRank = getLastRankWithLeagueAccountId(leagueAccountId);

      if(lastRank != null) {
        String finalQuery = String.format(UPDATE_LAST_RANK_SOLOQ_SECOND_WITH_ID, lastRankSoloqJson, lastRank.lastRank_id);
        query.execute(finalQuery);
      }
    }
  }
  
  public static void updateLastRankFlexWithLeagueAccountId(LeagueEntry flexRank, long leagueAccountId) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankFlexJson;

      if(flexRank == null) {
        lastRankFlexJson = "NULL";
      }else {
        lastRankFlexJson = gson.toJson(flexRank);
        lastRankFlexJson = "'" + lastRankFlexJson + "'";
      }
      
      DTO.LastRank lastRank = getLastRankWithLeagueAccountId(leagueAccountId);

      if(lastRank != null) {
        String finalQuery = String.format(UPDATE_LAST_RANK_FLEX_WITH_ID, lastRankFlexJson, lastRank.lastRank_id);
        query.execute(finalQuery);
      }
    }
  }
  
  public static void updateLastRankFlexSecondWithLeagueAccountId(LeagueEntry flexRank, long leagueAccountId) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankFlexJson;

      if(flexRank == null) {
        lastRankFlexJson = "NULL";
      }else {
        lastRankFlexJson = gson.toJson(flexRank);
        lastRankFlexJson = "'" + lastRankFlexJson + "'";
      }
      
      DTO.LastRank lastRank = getLastRankWithLeagueAccountId(leagueAccountId);

      if(lastRank != null) {
        String finalQuery = String.format(UPDATE_LAST_RANK_FLEX_SECOND_WITH_ID, lastRankFlexJson, lastRank.lastRank_id);
        query.execute(finalQuery);
      }
    }
  }
  
  public static void updateLastRankTftWithLeagueAccountId(LeagueEntry tftRank, long leagueAccountId) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankTftJson;

      if(tftRank == null) {
        lastRankTftJson = "NULL";
      }else {
        lastRankTftJson = gson.toJson(tftRank);
        lastRankTftJson = "'" + lastRankTftJson + "'";
      }
      
      DTO.LastRank lastRank = getLastRankWithLeagueAccountId(leagueAccountId);

      if(lastRank != null) {
        String finalQuery = String.format(UPDATE_LAST_RANK_TFT_WITH_ID, lastRankTftJson, lastRank.lastRank_id);
        query.execute(finalQuery);
      }
    }
  }
  
  public static void updateLastRankTftSecondWithLeagueAccountId(LeagueEntry tftRank, long leagueAccountId) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankTftJson;

      if(tftRank == null) {
        lastRankTftJson = "NULL";
      }else {
        lastRankTftJson = gson.toJson(tftRank);
        lastRankTftJson = "'" + lastRankTftJson + "'";
      }
      
      DTO.LastRank lastRank = getLastRankWithLeagueAccountId(leagueAccountId);

      if(lastRank != null) {
        String finalQuery = String.format(UPDATE_LAST_RANK_TFT_SECOND_WITH_ID, lastRankTftJson, lastRank.lastRank_id);
        query.execute(finalQuery);
      }
    }
  }
  
  public static void deleteLastRank(long lastRankId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(DELETE_LAST_RANK_WITH_ID, lastRankId);
      query.execute(finalQuery);
    }
  }
  
}
