package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.joda.time.DateTime;

public class ServerRepository {

  private static final String SELECT_SERVER_WITH_GUILDID = "SELECT serv_id, serv_language, serv_lastRefresh FROM server "
      + "WHERE serv_guildId = %d";
  
  private static final String INSERT_INTO_SERVER = "INSERT INTO server (serv_guildId, serv_language, serv_lastRefresh) "
      + "VALUES (%d, '%s', '%s')";
  
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
  
  public static void createNewServer(long guildId, String language, DateTime lastRefresh) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement statement = conn.createStatement();) {
      
      //Create Server
      String finalQuery = String.format(INSERT_INTO_SERVER, guildId, language, lastRefresh.toString());
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
  
}
