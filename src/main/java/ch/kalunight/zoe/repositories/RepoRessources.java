package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RepoRessources {

  public static String DB_USERNAME = "zoeadmin";
  public static String DB_PASSWORD = "xf345sD2#a@v";
  public static String DB_URL = "jdbc:postgresql://localhost:5432/zoe";
  
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
  
}
