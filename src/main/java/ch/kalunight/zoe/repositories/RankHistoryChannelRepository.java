package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.annotation.Nullable;
import ch.kalunight.zoe.model.dto.DTO;

public class RankHistoryChannelRepository {
  
  private static final String INSERT_RANK_HISTORY_CHANNEL = "INSERT INTO rank_history_channel " +
      "(rhChannel_fk_server, rhChannel_channelId) VALUES (%d, %d)";
  
  private static final String SELECT_RANK_HISTORY_CHANNEL_WITH_GUILDID = "SELECT " + 
      "rank_history_channel.rhchannel_id, " + 
      "rank_history_channel.rhchannel_fk_server, " + 
      "rank_history_channel.rhchannel_channelid " + 
      "FROM rank_history_channel " + 
      "INNER JOIN server ON rank_history_channel.rhchannel_fk_server = server.serv_id " + 
      "WHERE server.serv_guildid = %d";

  private static final String DELETE_RANK_HISTORY_CHANNEL = 
      "DELETE FROM rank_history_channel WHERE rhChannel_id = %d";
  
  private RankHistoryChannelRepository() {
    // hide default public constructor
  }
  
  public static void createRankHistoryChannel(long serverId, long channelId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(INSERT_RANK_HISTORY_CHANNEL, serverId, channelId);
      query.execute(finalQuery);
    }
  }
  
  @Nullable
  public static DTO.RankHistoryChannel getRankHistoryChannel(long guildId) throws SQLException{
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_RANK_HISTORY_CHANNEL_WITH_GUILDID, guildId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.RankHistoryChannel(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static void deleteRankHistoryChannel(long rankHistoryChannelId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(DELETE_RANK_HISTORY_CHANNEL, rankHistoryChannelId);
      query.execute(finalQuery);
    }
  }
  
}
