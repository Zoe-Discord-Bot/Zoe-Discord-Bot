package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.ZoePlatform;

public class BannedAccountRepository {

  private static final String INSERT_BANNED_ACCOUNT = "INSERT INTO banned_account " +
      "(banAcc_summonerId, banAcc_server) " +
      "VALUES ('%s', '%s')";
  
  private static final String SELECT_BANNED_ACCOUNT_WITH_SUMMONER_ID_AND_SERVER = "SELECT " + 
      "banned_account.banacc_id, " + 
      "banned_account.banacc_summonerid, " + 
      "banned_account.banacc_server " + 
      "FROM banned_account " + 
      "WHERE banned_account.banacc_summonerid = '%s' " + 
      "AND banned_account.banacc_server = '%s'";
  
  private static final String DELETE_BANNED_ACCOUNT_WITH_ID = "DELETE FROM banned_account WHERE banAcc_id = %d";
  
  private BannedAccountRepository() {
    // hide default public constructor
  }
  
  public static void createBannedAccount(String summonerId, ZoePlatform platform) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(INSERT_BANNED_ACCOUNT, summonerId, platform.getDbName());
      query.execute(finalQuery);
    }
  }
  
  public static DTO.BannedAccount getBannedAccount(String summonerId, ZoePlatform platform) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_BANNED_ACCOUNT_WITH_SUMMONER_ID_AND_SERVER, summonerId, platform.getDbName());
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
  
  public static void deleteBannedAccount(long bannedAccountId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(DELETE_BANNED_ACCOUNT_WITH_ID, bannedAccountId);
      query.executeUpdate(finalQuery);
    }
  }
  
}
