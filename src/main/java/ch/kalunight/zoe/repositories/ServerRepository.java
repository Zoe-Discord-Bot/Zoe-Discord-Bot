package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import ch.kalunight.zoe.model.dto.DTO;

public class ServerRepository {

  private static final String SELECT_SERVER_WITH_GUILDID = "SELECT serv_id, serv_guildId, serv_language, serv_lastRefresh FROM server "
      + "WHERE serv_guildId = %d";
  
  private static final String INSERT_INTO_SERVER = "INSERT INTO server (serv_guildId, serv_language, serv_lastRefresh) "
      + "VALUES (%d, '%s', '%s')";
  
  private static final String DELETE_SERVER_WITH_SERV_GUILDID = "DELETE FROM server WHERE serv_guildId = %d";
  
  private static final String UPDATE_LANGUAGE_WITH_GUILD_ID = "UPDATE server SET serv_language = '%s' WHERE serv_guildId = %d";
  
  private ServerRepository() {
    //Hide default public constructor
  }
  
  public static boolean checkServerExist(long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_SERVER_WITH_GUILDID, guildId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      
      return rowCount == 1;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static DTO.Server getServer(long guildId) throws SQLException{
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_SERVER_WITH_GUILDID, guildId);
      result = query.executeQuery(finalQuery);
      result.next();
      return new DTO.Server(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static void createNewServer(long guildId, String language) throws SQLException {
    LocalDateTime lastRefresh = LocalDateTime.now().minusMinutes(3);
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement statement = conn.createStatement();) {
      
      //Create Server
      String finalQuery = String.format(INSERT_INTO_SERVER, guildId, language, lastRefresh.format(DTO.DB_TIME_PATTERN));
      statement.executeUpdate(finalQuery);
      
      //Get serv_id from server
      finalQuery = String.format(SELECT_SERVER_WITH_GUILDID, guildId);
      result = statement.executeQuery(finalQuery);
      result.next();
      long servId = result.getLong("serv_id");
      result.close();
      
      ConfigRepository.initDefaultConfig(statement, servId);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static void deleteServer(long guildId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(DELETE_SERVER_WITH_SERV_GUILDID, guildId);
      query.execute(finalQuery);
    }
  }
  
  public static void updateLanguage(long guildId, String language) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(UPDATE_LANGUAGE_WITH_GUILD_ID, language, guildId);
      query.execute(finalQuery);
    }
  }
  
}
