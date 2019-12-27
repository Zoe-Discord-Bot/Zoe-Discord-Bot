package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.MessageChannel;

public class RepoRessources {

  public static String DB_USERNAME = "zoeadmin";
  public static String DB_PASSWORD = "";
  public static String DB_URL = "";
  
  private static final Logger logger = LoggerFactory.getLogger(RepoRessources.class);
  
  private RepoRessources() {
    //hide Repo Ressources
  }
  
  public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(RepoRessources.DB_URL, RepoRessources.DB_USERNAME, RepoRessources.DB_PASSWORD);
  }

  public static void closeResultSet(ResultSet result) throws SQLException {
    if(result != null && !result.isClosed()) {
      result.close();
    }
  }
  
  public static void sqlErrorReport(MessageChannel channel, DTO.Server server, SQLException e) {
    logger.error("SQL issue when updating option", e);
    channel.sendMessage(LanguageManager.getText(server.serv_language, "errorSQLPleaseReport")).complete();
  }
  
  public static void setDB_PASSWORD(String dB_PASSWORD) {
    DB_PASSWORD = dB_PASSWORD;
  }

  public static void setDB_URL(String dB_URL) {
    DB_URL = dB_URL;
  }
  
}
