package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.kalunight.zoe.model.dto.DTO;

public class LeaderboardRepository {

  private static final String INSERT_LEADERBOARD = "INSERT INTO leaderboard " +
      "(lead_fk_server, lead_message_channelId, " +
      "lead_message_id, lead_type) " +
      "VALUES (%d, %d, %d, '%s')";
  
  private static final String DELETE_LEADERBOARD_WITH_ID = "DELETE FROM leaderboard WHERE lead_id = %d";
  
  private static final String SELECT_LEADERBOARD_WITH_GUILD_ID = "SELECT " + 
      "leaderboard.lead_id, " + 
      "leaderboard.lead_fk_server, " + 
      "leaderboard.lead_message_channelid, " + 
      "leaderboard.lead_message_id, " + 
      "leaderboard.lead_type " + 
      "FROM server " + 
      "INNER JOIN leaderboard ON server.serv_id = leaderboard.lead_fk_server " + 
      "WHERE server.serv_guildid = %d";
  
  private LeaderboardRepository() {
    //hide default public constructor
  }
  
  public static void createLeaderboard(long guildId, long channelId, long messageId, String type) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(INSERT_LEADERBOARD, guildId, channelId, messageId, type);
      query.execute(finalQuery);
    }
  }

  public static void deleteLeaderboardWithId(long leadId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(DELETE_LEADERBOARD_WITH_ID, leadId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static List<DTO.Leaderboard> getLeaderboardWithId(long guildId) throws SQLException {
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
