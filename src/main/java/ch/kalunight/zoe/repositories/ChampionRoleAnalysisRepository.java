package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.ChampionRoleAnalysis;

public class ChampionRoleAnalysisRepository {

  private static final String SELECT_CHAMPION_ROLE_ANALYSIS_WITH_CHAMPION_ID = "SELECT " + 
      "champion_role_analysis.cra_id, " + 
      "champion_role_analysis.cra_keychampion, " + 
      "champion_role_analysis.cra_lastrefresh, " + 
      "champion_role_analysis.cra_roles, " +
      "champion_role_analysis.cra_roles_stats " + 
      "FROM champion_role_analysis " + 
      "WHERE champion_role_analysis.cra_keychampion = %d";
  
  private static final String INSERT_CHAMPION_ROLE_ANALYSIS = "INSERT INTO champion_role_analysis "
      + "(cra_keychampion, cra_lastrefresh, cra_roles, cra_roles_stats) VALUES (%d, '%s', '%s', '%s')";
  
  private static final String UPDATE_CHAMPION_ROLE_ANALYSIS = "UPDATE champion_role_analysis "
      + "SET cra_lastrefresh = '%s', cra_roles = '%s', cra_roles_stats = '%s' WHERE cra_keychampion = %d";
  
  private ChampionRoleAnalysisRepository() {
    //hide Repo Ressources
  }
  
  public static ChampionRoleAnalysis getChampionRoleAnalysis(int championId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_CHAMPION_ROLE_ANALYSIS_WITH_CHAMPION_ID, championId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.ChampionRoleAnalysis(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static void createChampionRoles(int championId, String roles, String stats) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(INSERT_CHAMPION_ROLE_ANALYSIS, championId,
          DTO.DB_TIME_PATTERN.format(LocalDateTime.now()), roles, stats);
      
      query.execute(finalQuery);
    }
  }
  
  public static void updateChampionsRoles(int championId, String roles, String stats) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(UPDATE_CHAMPION_ROLE_ANALYSIS,
          DTO.DB_TIME_PATTERN.format(LocalDateTime.now()), roles, stats, championId);
      
      query.execute(finalQuery);
    }
  }
  
}
