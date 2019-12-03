package ch.kalunight.zoe.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConfigRepository {

  private static final String INSERT_INTO_SERVER_CONFIGURATION = "INSERT INTO server_configuration (servConfig_fk_server) "
      + "VALUES (%d)";

  private static final String SELECT_SERVER_CONFIG_WITH_SERV_ID = "SELECT servConfig_id FROM server_configuration "
      + "WHERE servConfig_fk_server = %d";

  private static final String INSERT_INTO_GAME_INFO_CARD_OPTION = "INSERT INTO game_info_card_option "
      + "(gameCardOption_fk_serverConfig, gameCardOption_activate) VALUES (%d, %s)";


  private ConfigRepository() {
    //hide default constructor
  }

  public static void initDefaultConfig(Statement statement, long servId) throws SQLException {
    //Create ServerConfiguration
    String finalQuery = String.format(INSERT_INTO_SERVER_CONFIGURATION, servId);
    statement.execute(finalQuery);

    //Get servConfig_id from ServerConfiguration
    finalQuery = String.format(SELECT_SERVER_CONFIG_WITH_SERV_ID, servId);
    ResultSet result = null;
    long servConfigId;
    try {
      result = statement.executeQuery(finalQuery);
      result.next();
      servConfigId = result.getLong("servConfig_id");
    }finally {
      RepoRessources.closeResultSet(result);
    }
    
    //Create GameInfoCardOption
    finalQuery = String.format(INSERT_INTO_GAME_INFO_CARD_OPTION, servConfigId, "TRUE");
    statement.execute(finalQuery);
  }

}
