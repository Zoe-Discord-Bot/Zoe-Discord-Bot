package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Nullable;

import ch.kalunight.zoe.model.dto.DTO;
import net.rithms.riot.constant.Platform;

public class ClashTournamentRepository {
  
  private static final String SELECT_CLASH_TOURNAMENT_WITH_REGION = "SELECT " + 
      "SELECT " + 
      "clash_tournament_cache.clashtourcache_id, " + 
      "clash_tournament_cache.clashtourcache_server, " + 
      "clash_tournament_cache.clashtourcache_data " + 
      "FROM clash_tournament_cache " + 
      "WHERE clash_tournament_cache.clashtourcache_server = '%s'";
  
  private ClashTournamentRepository() {
    //hide default public constructor
  }
  
  @Nullable
  public static DTO.ClashTournamentCache getCurrentGameWithServerAndGameId(Platform platform) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_CLASH_TOURNAMENT_WITH_REGION, platform.getName());
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.ClashTournamentCache(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
}
