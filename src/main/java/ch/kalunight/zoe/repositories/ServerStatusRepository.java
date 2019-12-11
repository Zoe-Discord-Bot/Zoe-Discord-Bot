package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import ch.kalunight.zoe.model.dto.DTO;

public class ServerStatusRepository {

  private static final String SELECT_SERVER_STATUS_BY_GUILD_ID = "SELECT " + 
      "server_status.servstatus_id, " + 
      "server_status.servstatus_fk_server, " + 
      "server_status.servstatus_intreatment " + 
      "FROM server " + 
      "INNER JOIN server_status ON server.serv_id = server_status.servstatus_fk_server " + 
      "WHERE server.serv_guildid = %d";

  private static final String UPDATE_IN_TREATMENT_WITH_SERVER_STATUS_ID = "UPDATE server_status SET servstatus_intreatment = %s " +
      "WHERE servstatus_id = %d";

  private ServerStatusRepository() {
    //hide default public constructor
  }

  public static void updateInTreatment(long serverStatusId, boolean inTreatment) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(UPDATE_IN_TREATMENT_WITH_SERVER_STATUS_ID, inTreatment, serverStatusId);
      query.executeUpdate(finalQuery);
    }
  }

  public static DTO.ServerStatus getServerStatus(long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_SERVER_STATUS_BY_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);
      result.next();
      return new DTO.ServerStatus(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
}
