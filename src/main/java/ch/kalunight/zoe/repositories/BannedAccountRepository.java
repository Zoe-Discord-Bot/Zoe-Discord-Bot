package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ch.kalunight.zoe.model.dto.DTO;
import net.rithms.riot.constant.Platform;

public class BannedAccountRepository {

  private static final String INSERT_BANNED_ACCOUNT = "INSERT INTO banned_account " +
      "(banAcc_summonerdId, banAcc_server) " +
      "VALUES ('%s', '%s')";
  
  private static final String SELECT_BANNED_ACCOUNT_WITH_SUMMONER_ID_AND_SERVER = "SELECT " + 
      "banned_account.banacc_id, " + 
      "banned_account.banacc_summonerid, " + 
      "banned_account.banacc_server " + 
      "FROM banned_account " + 
      "WHERE banned_account.banacc_summonerid = '%s' " + 
      "AND banned_account.banacc_server = '%s'";
  
  private BannedAccountRepository() {
    // hide default public constructor
  }
  
  public static void createBannedAccount(String summonerId, Platform platform) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(INSERT_BANNED_ACCOUNT, summonerId, platform);
      query.executeQuery(finalQuery);

    }
  }
  
  public static DTO.BannedAccount getBannedAccount(String summonerId, Platform platform) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_BANNED_ACCOUNT_WITH_SUMMONER_ID_AND_SERVER, summonerId, platform.getName());
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.BannedAccount(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
}
