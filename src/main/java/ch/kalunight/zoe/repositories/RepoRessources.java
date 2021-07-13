package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class RepoRessources {

  public static final String DB_USERNAME = "zoeadmin";
  
  private static HikariDataSource dataSource;

  private static final Logger logger = LoggerFactory.getLogger(RepoRessources.class);
  
  private RepoRessources() {
    //hide Repo Ressources
  }
  
  public static void setupDatabase(String password, String url) {
    PGSimpleDataSource source = new PGSimpleDataSource();
    source.setURL(url);
    source.setDatabaseName("zoe");
    source.setUser(DB_USERNAME); 
    source.setPassword(password);
    
    HikariConfig config = new HikariConfig();
    config.setDataSource(source);
    config.setAutoCommit(true);
    config.setLeakDetectionThreshold(20000);
    
    dataSource = new HikariDataSource(config);
  }
  
  public static Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public static void closeResultSet(ResultSet result) throws SQLException {
    if(result != null && !result.isClosed()) {
      result.close();
    }
  }
  
  public static void sqlErrorReport(MessageChannel channel, DTO.Server server, SQLException e) {
    logger.error("SQL issue when updating option", e);
    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).complete();
  }
  
  public static void sqlErrorReportSlashResponse(SlashCommandEvent event, DTO.Server server, SQLException e) {
    logger.error("SQL issue when updating option", e);
    event.getHook().editOriginal(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).queue();
  }
  
  public static void shutdownDB() {
    dataSource.close();
  }
  
  public static HikariDataSource getDataSource() {
    return dataSource;
  }
  
}
