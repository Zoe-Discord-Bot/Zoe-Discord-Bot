package ch.kalunight.zoe.model.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.joda.time.DateTime;

public class DTO {
  
  private DTO() {
    //hide default constructor
  }
  
  public static class Server {
    public long serv_id;
    public long serv_guildId;
    public String serv_language;
    public DateTime serv_lastRefresh;
    
    public Server(ResultSet baseData) throws SQLException {
      serv_id = baseData.getLong("serv_id");
      serv_guildId = baseData.getLong("serv_guildId");
      serv_language = baseData.getString("serv_language");
      serv_lastRefresh = DateTime.parse(baseData.getString("serv_lastRefresh"));
    }
  }
  
}
