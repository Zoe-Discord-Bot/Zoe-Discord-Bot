package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;

public class ServerRepository {

  private static final String SELECT_SERVER_WITH_GUILDID = "SELECT serv_id, serv_guildId, serv_language, serv_lastRefresh FROM server "
      + "WHERE serv_guildId = ?";
  
  private static final String SELECT_SERVER_WITH_SERV_ID = "SELECT serv_id, serv_guildId, serv_language, serv_lastRefresh FROM server "
      + "WHERE serv_id = ?";
  
  private static final String SELECT_ALL_SERVERS = "SELECT serv_id, serv_guildId, serv_language, serv_lastRefresh FROM server";
  
  private static final String INSERT_INTO_SERVER = "INSERT INTO server (serv_guildId, serv_language, serv_lastRefresh) "
      + "VALUES (%d, '%s', '%s')";
  
  private static final String INSERT_INTO_SERVER_STATUS = "INSERT INTO server_status (servStatus_fk_server)"
      + "VALUES (%d)";
  
  private static final String DELETE_SERVER_WITH_SERV_GUILDID = "DELETE FROM server WHERE serv_guildId = %d";
  
  private static final String UPDATE_LANGUAGE_WITH_GUILD_ID = "UPDATE server SET serv_language = '%s' WHERE serv_guildId = %d";
  
  private static final String UPDATE_TIMESTAMP_WITH_GUILD_ID = "UPDATE server SET serv_lastrefresh = '%s' WHERE serv_guildId = %d";
  
  private static final String SELECT_SERVER_WITH_TIMESTAMP_AFTER = "SELECT " + 
      "server.serv_id,server.serv_guildid,server.serv_language,server.serv_lastrefresh " + 
      "FROM info_channel " + 
      "RIGHT JOIN server ON info_channel.infochannel_fk_server = server.serv_id " + 
      "LEFT JOIN rank_history_channel ON server.serv_id = rank_history_channel.rhchannel_fk_server " + 
      "INNER JOIN server_status ON server.serv_id = server_status.servstatus_fk_server " + 
      "WHERE server.serv_lastrefresh < '%s' " + 
      "AND (rank_history_channel.rhchannel_channelid IS NOT NULL OR info_channel.infochannel_channelid IS NOT NULL) " + 
      "AND server_status.servstatus_intreatment = %s";
  
  private ServerRepository() {
    //Hide default public constructor
  }
  
  public static boolean checkServerExist(long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        PreparedStatement stmt = conn.prepareStatement(SELECT_SERVER_WITH_GUILDID, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      stmt.setLong(1, guildId);
      result = stmt.executeQuery();
      int rowCount = result.last() ? result.getRow() : 0;
      
      return rowCount == 1;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static DTO.Server getServerWithGuildId(long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        PreparedStatement stmt = conn.prepareStatement(SELECT_SERVER_WITH_GUILDID, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      stmt.setLong(1, guildId);
      result = stmt.executeQuery();
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.Server(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static DTO.Server getServerWithServId(long servId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        PreparedStatement stmt = conn.prepareStatement(SELECT_SERVER_WITH_SERV_ID, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      stmt.setLong(1, servId);
      result = stmt.executeQuery();
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.Server(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.Server> getAllServers() throws SQLException {
    List<DTO.Server> servers = new ArrayList<>();
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SERVERS, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);){
      
      result = stmt.executeQuery();
      result.first();
      while(!result.isAfterLast()) {
        servers.add(new DTO.Server(result));
        result.next();
      }
      return servers;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static void createNewServer(long guildId, String language) throws SQLException {
    LocalDateTime lastRefresh = LocalDateTime.now().minusMinutes(3);
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement statement = conn.createStatement();
        PreparedStatement stmt = conn.prepareStatement(SELECT_SERVER_WITH_GUILDID, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      //Create Server
      String finalQuery = String.format(INSERT_INTO_SERVER, guildId, language, lastRefresh.format(DTO.DB_TIME_PATTERN));
      statement.executeUpdate(finalQuery);
      
      //Get serv_id from server
      stmt.setLong(1, guildId);
      result = stmt.executeQuery();
      result.next();
      long servId = result.getLong("serv_id");
      result.close();
      
      ConfigRepository.initDefaultConfig(statement, servId);
      
      //Create Server status
      finalQuery = String.format(INSERT_INTO_SERVER_STATUS, servId);
      statement.execute(finalQuery);
      
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static void deleteServer(long guildId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      DTO.Server server = ServerRepository.getServerWithGuildId(guildId);
      
      List<DTO.Player> players = PlayerRepository.getPlayers(guildId);
      
      for(DTO.Player player : players) {
        PlayerRepository.deletePlayer(player.player_id, guildId);
      }
      
      InfoChannelRepository.deleteInfoChannel(server);
      
      DTO.RankHistoryChannel rankHistoryChannel = RankHistoryChannelRepository.getRankHistoryChannel(guildId);
      
      if(rankHistoryChannel != null) {
        RankHistoryChannelRepository.deleteRankHistoryChannel(rankHistoryChannel.rhChannel_id);
      }
      
      List<DTO.Team> teams = TeamRepository.getTeamsByGuild(guildId);
      for(DTO.Team team : teams) {
        TeamRepository.deleteTeam(team.team_id, new ArrayList<>());
      }
      
      List<DTO.Leaderboard> leaderboards = LeaderboardRepository.getLeaderboardsWithGuildId(guildId);
      for(Leaderboard leaderboard : leaderboards) {
        LeaderboardRepository.deleteLeaderboardWithId(leaderboard.lead_id);
      }

      String finalQuery = String.format(DELETE_SERVER_WITH_SERV_GUILDID, guildId);
      query.execute(finalQuery);
    }
  }
  
  public static void updateLanguage(long guildId, String language) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(UPDATE_LANGUAGE_WITH_GUILD_ID, language, guildId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static void updateTimeStamp(long guildId, LocalDateTime timeStamp) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(UPDATE_TIMESTAMP_WITH_GUILD_ID, DTO.DB_TIME_PATTERN.format(timeStamp), guildId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static List<DTO.Server> getGuildWhoNeedToBeRefresh() throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_SERVER_WITH_TIMESTAMP_AFTER,
          DTO.DB_TIME_PATTERN.format(LocalDateTime.now().minusMinutes(5)),
          false);
      result = query.executeQuery(finalQuery);
      
      List<DTO.Server> servers = new ArrayList<>();
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return servers;
      }
      
      result.first();
      while(!result.isAfterLast()) {
        servers.add(new DTO.Server(result));
        result.next();
      }
      
      return servers;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
}
