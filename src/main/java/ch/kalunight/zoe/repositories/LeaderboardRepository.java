package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.kalunight.zoe.model.dto.DTO;

public class LeaderboardRepository {

  private static final String INSERT_LEADERBOARD = "INSERT INTO leaderboard " +
      "(lead_fk_server, lead_message_channelId, " +
      "lead_message_id, lead_type, lead_lastrefresh) " +
      "VALUES (%d, %d, %d, '%s', '%s') " +
      "RETURNING lead_id";
  
  private static final String DELETE_LEADERBOARD_WITH_ID = "DELETE FROM leaderboard WHERE lead_id = %d";
  
  private static final String UPDATE_LEADERBOARD_DATA_WITH_ID = "UPDATE leaderboard SET lead_data = %s WHERE lead_id = %d";
  
  private static final String UPDATE_LEADERBOARD_LAST_REFRESH_WITH_ID = "UPDATE leaderboard SET lead_lastrefresh = '%s' WHERE lead_id = %d";
  
  private static final String SELECT_LEADERBOARD_WITH_GUILD_ID = "SELECT " + 
      "leaderboard.lead_id, " + 
      "leaderboard.lead_fk_server, " + 
      "leaderboard.lead_message_channelid, " + 
      "leaderboard.lead_message_id, " + 
      "leaderboard.lead_type," +
      "leaderboard.lead_data," +
      "leaderboard.lead_lastrefresh " + 
      "FROM server " + 
      "INNER JOIN leaderboard ON server.serv_id = leaderboard.lead_fk_server " + 
      "WHERE server.serv_guildid = %d";
  
  private static final String SELECT_LEADERBOARD_WITH_ID = "SELECT " + 
      "leaderboard.lead_id, " + 
      "leaderboard.lead_fk_server, " + 
      "leaderboard.lead_message_channelid, " + 
      "leaderboard.lead_message_id, " + 
      "leaderboard.lead_type, " + 
      "leaderboard.lead_data, " + 
      "leaderboard.lead_lastrefresh " + 
      "FROM leaderboard " + 
      "WHERE leaderboard.lead_id = %d";
  
  private static final String SELECT_LEADERBOARD_WITH_LAST_REFRESH = "SELECT " + 
      "leaderboard.lead_id, " + 
      "leaderboard.lead_fk_server, " + 
      "leaderboard.lead_message_channelid, " + 
      "leaderboard.lead_message_id, " + 
      "leaderboard.lead_type, " + 
      "leaderboard.lead_data, " + 
      "leaderboard.lead_lastrefresh " + 
      "FROM leaderboard " + 
      "WHERE leaderboard.lead_lastrefresh < '%s'";
  
  private static final String COUNT_LEADERBOARDS = "SELECT COUNT(*) FROM leaderboard";
  
  private LeaderboardRepository() {
    //hide default public constructor
  }
  
  public static long countLeaderboards() throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      result = query.executeQuery(COUNT_LEADERBOARDS);
      
      result.first();
      
      return result.getLong("count");
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.Leaderboard> getLeaderboardWhoNeedToBeRefreshed() throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_LEADERBOARD_WITH_LAST_REFRESH,
          DTO.DB_TIME_PATTERN.format(LocalDateTime.now().minusHours(1)));
      result = query.executeQuery(finalQuery);
      
      List<DTO.Leaderboard> leaderboards = new ArrayList<>();
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return leaderboards;
      }
      
      result.first();
      while(!result.isAfterLast()) {
        leaderboards.add(new DTO.Leaderboard(result));
        result.next();
      }
      
      return leaderboards;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static DTO.Leaderboard createLeaderboard(long serverId, long channelId, long messageId, int type) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(INSERT_LEADERBOARD, serverId, channelId,
          messageId, type, DTO.DB_TIME_PATTERN.format(LocalDateTime.now()));
      result = query.executeQuery(finalQuery);
      result.next();
      
      return getLeaderboardWithId(result.getLong("lead_id"));
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static void deleteLeaderboardWithId(long leadId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(DELETE_LEADERBOARD_WITH_ID, leadId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static void updateLeaderboardDataWithLeadId(long leaderboardId, String data) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery;
      if(data == null) {
        finalQuery = String.format(UPDATE_LEADERBOARD_DATA_WITH_ID, "NULL", leaderboardId);
      }else {
        finalQuery = String.format(UPDATE_LEADERBOARD_DATA_WITH_ID, "'" + data + "'", leaderboardId);
      }
      query.executeUpdate(finalQuery);
    }
  }
  
  public static void updateLeaderboardLastRefreshWithLeadId(long leaderboardId, LocalDateTime refreshTime) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_LEADERBOARD_LAST_REFRESH_WITH_ID, DTO.DB_TIME_PATTERN.format(refreshTime), leaderboardId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static DTO.Leaderboard getLeaderboardWithId(long leaderboardId) throws SQLException{
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_LEADERBOARD_WITH_ID, leaderboardId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.Leaderboard(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.Leaderboard> getLeaderboardsWithGuildId(long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_LEADERBOARD_WITH_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);
      List<DTO.Leaderboard> leaderboards = Collections.synchronizedList(new ArrayList<>());
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return leaderboards;
      }
      result.first();
      while(!result.isAfterLast()) {
        leaderboards.add(new DTO.Leaderboard(result));
        result.next();
      }

      return leaderboards;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
}
