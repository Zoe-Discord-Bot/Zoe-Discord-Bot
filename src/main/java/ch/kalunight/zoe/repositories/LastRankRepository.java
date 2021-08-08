package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;

public class LastRankRepository {

  private static final String INSERT_LAST_RANK = "INSERT INTO last_rank " +
      "(lastRank_fk_leagueAccount) VALUES (%d)";

  private static final String SELECT_LAST_RANK_WITH_LEAGUE_ACCOUNT_ID = "SELECT " + 
      "last_rank.lastrank_id, " + 
      "last_rank.lastrank_fk_leagueaccount, " + 
      "last_rank.lastrank_soloq, " +
      "last_rank.lastrank_soloqSecond, " + 
      "last_rank.lastrank_soloqLastRefresh, " + 
      "last_rank.lastrank_flex, " + 
      "last_rank.lastrank_flexSecond, " + 
      "last_rank.lastrank_flexLastRefresh, " + 
      "last_rank.lastrank_tft, " + 
      "last_rank.lastrank_tftSecond, " + 
      "last_rank.lastrank_tftLastRefresh, " + 
      "last_rank.lastrank_tftlasttreatedmatchid " +
      "FROM league_account " + 
      "INNER JOIN last_rank ON league_account.leagueaccount_id = last_rank.lastrank_fk_leagueaccount " + 
      "WHERE league_account.leagueaccount_id = %d";

  private static final String UPDATE_LAST_RANK_SOLOQ_WITH_ID = 
      "UPDATE last_rank SET lastRank_soloq = %s, lastrank_soloqLastRefresh = '%s' WHERE lastRank_id = %d";

  private static final String UPDATE_LAST_RANK_SOLOQ_SECOND_WITH_ID = 
      "UPDATE last_rank SET lastRank_soloqSecond = %s, lastrank_soloqLastRefresh = '%s' WHERE lastRank_id = %d";

  private static final String UPDATE_LAST_RANK_FLEX_WITH_ID = 
      "UPDATE last_rank SET lastRank_flex = %s, lastrank_flexLastRefresh = '%s' WHERE lastRank_id = %d";

  private static final String UPDATE_LAST_RANK_FLEX_SECOND_WITH_ID = 
      "UPDATE last_rank SET lastRank_flexSecond = %s, lastrank_flexLastRefresh = '%s' WHERE lastRank_id = %d";

  private static final String UPDATE_LAST_RANK_TFT_WITH_ID = 
      "UPDATE last_rank SET lastRank_tft = %s, lastrank_tftLastRefresh = '%s' WHERE lastRank_id = %d";

  private static final String UPDATE_LAST_RANK_TFT_SECOND_WITH_ID = 
      "UPDATE last_rank SET lastRank_tftSecond = %s, lastrank_tftLastRefresh = '%s' WHERE lastRank_id = %d";

  private static final String UPDATE_LAST_RANK_TFT_MATCH_ID =
      "UPDATE last_rank SET lastRank_tftLastTreatedMatchId = '%s' WHERE lastRank_id = %d";

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
    try (Connection conn = RepoRessources.getConnection();) {
      return getLastRankWithLeagueAccountId(leagueAccountId, conn);
    }
  }
  
  public static DTO.LastRank getLastRankWithLeagueAccountId(long leagueAccountId, Connection conn) throws SQLException{
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

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

  public static void updateLastRankSoloqWithLeagueAccountId(LeagueEntry soloqRank, LastRank lastRank, LocalDateTime refreshTime) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankSoloqJson;

      if(soloqRank == null) {
        lastRankSoloqJson = "NULL";
      }else {
        lastRankSoloqJson = gson.toJson(soloqRank);
        lastRankSoloqJson = "'" + lastRankSoloqJson + "'";
      }
      
      String refreshTimeFormated = DTO.DB_TIME_PATTERN.format(refreshTime);

      String finalQuery = String.format(UPDATE_LAST_RANK_SOLOQ_WITH_ID, lastRankSoloqJson, refreshTimeFormated, lastRank.lastRank_id);
      query.execute(finalQuery);

    }
  }

  public static void updateLastRankSoloqSecondWithLeagueAccountId(LeagueEntry soloqRank, LastRank lastRank, LocalDateTime refreshTime) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankSoloqJson;

      if(soloqRank == null) {
        lastRankSoloqJson = "NULL";
      }else {
        lastRankSoloqJson = gson.toJson(soloqRank);
        lastRankSoloqJson = "'" + lastRankSoloqJson + "'";
      }

      String refreshTimeFormated = DTO.DB_TIME_PATTERN.format(refreshTime);
      
      String finalQuery = String.format(UPDATE_LAST_RANK_SOLOQ_SECOND_WITH_ID, lastRankSoloqJson, refreshTimeFormated, lastRank.lastRank_id);
      query.execute(finalQuery);
    }
  }

  public static void updateLastRankFlexWithLeagueAccountId(LeagueEntry flexRank, LastRank lastRank, LocalDateTime refreshTime) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankFlexJson;

      if(flexRank == null) {
        lastRankFlexJson = "NULL";
      }else {
        lastRankFlexJson = gson.toJson(flexRank);
        lastRankFlexJson = "'" + lastRankFlexJson + "'";
      }
      
      String refreshTimeFormated = DTO.DB_TIME_PATTERN.format(refreshTime);

      String finalQuery = String.format(UPDATE_LAST_RANK_FLEX_WITH_ID, lastRankFlexJson, refreshTimeFormated, lastRank.lastRank_id);
      query.execute(finalQuery);
    }
  }

  public static void updateLastRankFlexSecondWithLeagueAccountId(LeagueEntry flexRank, LastRank lastRank, LocalDateTime refreshTime) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankFlexJson;

      if(flexRank == null) {
        lastRankFlexJson = "NULL";
      }else {
        lastRankFlexJson = gson.toJson(flexRank);
        lastRankFlexJson = "'" + lastRankFlexJson + "'";
      }

      String refreshTimeFormated = DTO.DB_TIME_PATTERN.format(refreshTime);
      
      String finalQuery = String.format(UPDATE_LAST_RANK_FLEX_SECOND_WITH_ID, lastRankFlexJson, refreshTimeFormated, lastRank.lastRank_id);
      query.execute(finalQuery);
    }
  }

  public static void updateLastRankTftWithLeagueAccountId(LeagueEntry tftRank, LastRank lastRank, LocalDateTime refreshTime) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankTftJson;

      if(tftRank == null) {
        lastRankTftJson = "NULL";
      }else {
        lastRankTftJson = gson.toJson(tftRank);
        lastRankTftJson = "'" + lastRankTftJson + "'";
      }

      String refreshTimeFormated = DTO.DB_TIME_PATTERN.format(refreshTime);
      
      String finalQuery = String.format(UPDATE_LAST_RANK_TFT_WITH_ID, lastRankTftJson, refreshTimeFormated, lastRank.lastRank_id);
      query.execute(finalQuery);
    }
  }

  public static void updateLastRankTftSecondWithLeagueAccountId(LeagueEntry tftRank, LastRank lastRank, LocalDateTime refreshTime) throws SQLException{
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String lastRankTftJson;

      if(tftRank == null) {
        lastRankTftJson = "NULL";
      }else {
        lastRankTftJson = gson.toJson(tftRank);
        lastRankTftJson = "'" + lastRankTftJson + "'";
      }
      
      String refreshTimeFormated = DTO.DB_TIME_PATTERN.format(refreshTime);

      String finalQuery = String.format(UPDATE_LAST_RANK_TFT_SECOND_WITH_ID, lastRankTftJson, refreshTimeFormated,lastRank.lastRank_id);
      query.execute(finalQuery);
    }
  }

  public static void updateLastRankTFTLastTreatedMatch(String matchid, LastRank lastRank) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_LAST_RANK_TFT_MATCH_ID, matchid, lastRank.lastRank_id);
      query.execute(finalQuery);
    }
  }
  
  public static void deleteLastRank(long lastRankId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      deleteLastRank(lastRankId, conn);
    }
  }

  public static void deleteLastRank(long lastRankId, Connection conn) throws SQLException {
    try (Statement query = conn.createStatement();) {

      String finalQuery = String.format(DELETE_LAST_RANK_WITH_ID, lastRankId);
      query.execute(finalQuery);
    }
  }

}
