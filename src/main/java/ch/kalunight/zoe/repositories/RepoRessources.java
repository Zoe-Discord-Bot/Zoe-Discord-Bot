package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.config.option.CleanChannelOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.MessageChannel;

public class RepoRessources {

  public static String DB_USERNAME = "zoeadmin";
  public static String DB_PASSWORD = "xf345sD2#a@v";
  public static String DB_URL = "jdbc:postgresql://localhost:5432/zoe";
  
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
  
}
