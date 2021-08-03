package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.model.dto.DTO;

public class ZoeUserManagementRepository {

  private static final String SELECT_ZOE_USER_BY_DISCORD_ID = "SELECT "
      + "zoe_user.zoeuser_id, "
      + "zoe_user.zoeuser_discordid, "
      + "zoe_user.zoeuser_fullmonthsupported, "
      + "zoe_user.zoeuser_totalgiven "
      + "FROM zoe_user "
      + "WHERE zoe_user.zoeuser_discordid = %d";
  
  private static final String SELECT_ZOE_SUBSCRIPTION_BY_DISCORD_ID = "SELECT "
      + "zoe_user_role.zoeuserrole_fk_user_id, "
      + "zoe_user_role.zoeuserrole_fk_role_id, "
      + "zoe_user_role.zoeuserrole_endofthesubscription "
      + "FROM zoe_user "
      + "INNER JOIN zoe_user_role ON zoe_user.zoeuser_id = zoe_user_role.zoeuserrole_fk_user_id "
      + "WHERE zoe_user.zoeuser_discordid = %d";
  
  private static final String SELECT_ZOE_SUBSCRIPTION_BY_DISCORD_ID_AND_ROLE = "SELECT "
      + "zoe_user_role.zoeuserrole_fk_user_id, "
      + "zoe_user_role.zoeuserrole_fk_role_id, "
      + "zoe_user_role.zoeuserrole_endofthesubscription "
      + "FROM zoe_user "
      + "INNER JOIN zoe_user_role ON zoe_user.zoeuser_id = zoe_user_role.zoeuserrole_fk_user_id "
      + "INNER JOIN role ON zoe_user_role.zoeuserrole_fk_role_id = role.role_id "
      + "WHERE role.role_roleid = %d "
      + "AND zoe_user.zoeuser_discordid = %d";
  
  private static final String SELECT_ALL_ROLES = "SELECT "
      + "role.role_id,role.role_roleid "
      + "FROM role";
  
  private static final String SELECT_SUBSCRIPTIONS_BY_ROLE = "SELECT "
      + "zoe_user_role.zoeuserrole_fk_user_id, "
      + "zoe_user_role.zoeuserrole_fk_role_id, "
      + "zoe_user_role.zoeuserrole_endofthesubscription "
      + "FROM zoe_user_role "
      + "INNER JOIN role ON zoe_user_role.zoeuserrole_fk_role_id = role.role_id "
      + "WHERE role.role_roleid = %d";
  
  private static final String INSERT_INTO_ZOE_USER = "INSERT INTO zoe_user " +
      "(zoeUser_discordId) VALUES (%d)";
  
  private static final String UPDATE_ZOE_USER = 
      "UPDATE zoe_user SET zoeUser_fullMonthSupported = %d, zoeUser_totalGiven = %d " +
      "WHERE zoeUser_discordId = %d";

  private static final String INSERT_INTO_ZOE_SUBSCRIPTION = "INSERT INTO zoe_user_role " +
      "(zoeUserRole_fk_user_id, zoeUserRole_fk_role_id, zoeUserRole_endOfTheSubscription) " +
      "VALUES (%d, %d, %s)";
  
  private static final String UPDATE_ZOE_SUBSCRIPTION = 
      "UPDATE zoe_user_role SET zoeUserRole_endOfTheSubscription = %s " +
      "WHERE zoeUserRole_fk_user_id = %d AND zoeUserRole_fk_role_id = %d";
  
  private static final String INSERT_INTO_ROLE = "INSERT INTO role " +
      "(role_roleId) VALUES (%d)";
  
  private static final String UPDATE_ZOE_ROLE = 
      "UPDATE role SET role_roleId = %d " +
      "WHERE role_id = %d";
  
  public static DTO.ZoeUser getZoeUserByDiscordId(Long discordId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_ZOE_USER_BY_DISCORD_ID, discordId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.ZoeUser(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static void createZoeRole(long roleId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(INSERT_INTO_ROLE, roleId);
      query.execute(finalQuery);
    }
  }
  
  public static void updateZoeRole(long id, long roleId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_ZOE_ROLE, roleId, id);
      query.execute(finalQuery);
    }
  }
  
  public static void createZoeSubscription(long zoeUserId, long roleId, LocalDateTime endOfSub) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String endOfSubTimestamp;
      if(endOfSub == null) {
        endOfSubTimestamp = "NULL";
      }else {
        endOfSubTimestamp = "'" + DTO.DB_TIME_PATTERN.format(endOfSub) + "'";
      }
      String finalQuery = String.format(INSERT_INTO_ZOE_SUBSCRIPTION,
          zoeUserId, roleId, endOfSubTimestamp);
      query.execute(finalQuery);
    }
  }
  
  public static void updateZoeSubscription(long zoeUserId, long roleId, LocalDateTime endOfSub) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String endOfSubTimestamp;
      if(endOfSub == null) {
        endOfSubTimestamp = "NULL";
      }else {
        endOfSubTimestamp = "'" + DTO.DB_TIME_PATTERN.format(endOfSub) + "'";
      }
      String finalQuery = String.format(UPDATE_ZOE_SUBSCRIPTION, endOfSubTimestamp, zoeUserId, roleId);
      query.execute(finalQuery);
    }
  }
  
  public static void createZoeUser(long discordId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(INSERT_INTO_ZOE_USER, discordId);
      query.execute(finalQuery);
    }
  }
  
  public static void updateZoeUser(long discordId, long fullMonthOfSub, long totalGiven) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_ZOE_USER, fullMonthOfSub, totalGiven, discordId);
      query.execute(finalQuery);
    }
  }
  
  public static List<DTO.ZoeUserRole> getZoeSubscriptionByDiscordId(Long discordId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_ZOE_SUBSCRIPTION_BY_DISCORD_ID, discordId);
      result = query.executeQuery(finalQuery);
      
      List<DTO.ZoeUserRole> subscription = new ArrayList<>();
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return subscription;
      }
      
      result.first();
      while(!result.isAfterLast()) {
        subscription.add(new DTO.ZoeUserRole(result));
        result.next();
      }
      
      return subscription;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.ZoeUserRole> getZoeSubscriptionByDiscordIdAndRole(Long discordId, Long roleId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_ZOE_SUBSCRIPTION_BY_DISCORD_ID_AND_ROLE, 
          roleId, discordId);
      result = query.executeQuery(finalQuery);
      
      List<DTO.ZoeUserRole> subscription = new ArrayList<>();
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return subscription;
      }
      
      result.first();
      while(!result.isAfterLast()) {
        subscription.add(new DTO.ZoeUserRole(result));
        result.next();
      }
      
      return subscription;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.Role> getAllZoeRole() throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_ALL_ROLES);
      result = query.executeQuery(finalQuery);
      
      List<DTO.Role> roles = new ArrayList<>();
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return roles;
      }
      
      result.first();
      while(!result.isAfterLast()) {
        roles.add(new DTO.Role(result));
        result.next();
      }
      
      return roles;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.ZoeUserRole> getZoeSubscriptionByRole(long roleId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_SUBSCRIPTIONS_BY_ROLE);
      result = query.executeQuery(finalQuery);
      
      List<DTO.ZoeUserRole> subscription = new ArrayList<>();
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return subscription;
      }
      
      result.first();
      while(!result.isAfterLast()) {
        subscription.add(new DTO.ZoeUserRole(result));
        result.next();
      }
      
      return subscription;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  
}
