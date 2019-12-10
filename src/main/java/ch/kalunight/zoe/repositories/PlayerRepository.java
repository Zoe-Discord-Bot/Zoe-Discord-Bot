package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import ch.kalunight.zoe.model.dto.DTO;

public class PlayerRepository {

  private static final String SELECT_PLAYER_WITH_DISCORD_ID = "SELECT " + 
      "player.player_id, " + 
      "player.player_fk_server, " + 
      "player.player_fk_team, " + 
      "player.player_discordid, " + 
      "player.player_mentionnable " + 
      "FROM server " + 
      "INNER JOIN player ON server.serv_id = player.player_fk_server " + 
      "WHERE server.serv_guildid = %d " + 
      "AND player.player_discordid = %d";
  
  private static final String SELECT_PLAYERS_WITH_GUILD_ID = "SELECT " + 
      "player.player_id, " + 
      "player.player_fk_server, " + 
      "player.player_fk_team, " + 
      "player.player_discordid, " + 
      "player.player_mentionnable " + 
      "FROM server " + 
      "INNER JOIN player ON server.serv_id = player.player_fk_server " + 
      "WHERE server.serv_guildid = %d";
  
  private static final String SELECT_PLAYER_WITH_GUILD_ID_AND_LEAGUE_ACCOUNT = "SELECT " + 
      "player.player_id, " + 
      "player.player_fk_server, " + 
      "player.player_fk_team, " + 
      "player.player_discordid, " + 
      "player.player_mentionnable " + 
      "FROM server " + 
      "INNER JOIN player ON server.serv_id = player.player_fk_server " + 
      "INNER JOIN league_account ON player.player_id = league_account.leagueaccount_fk_player " + 
      "WHERE server.serv_guildid = %d " + 
      "AND league_account.leagueaccount_summonerid = '%s' " + 
      "AND league_account.leagueaccount_server = '%s'";
  
  private static final String INSERT_INTO_PLAYER = "INSERT INTO player " +
      "(player_fk_server, player_discordId, player_mentionnable) VALUES (%d, %d, %s)";
  
  private static final String DELETE_PLAYER_WITH_PLAYER_ID = "DELETE FROM player WHERE player_id = %d";
  
  private static final String UPDATE_TEAM_OF_PLAYER = "UPDATE player SET player_fk_team = %d WHERE player_id = %d";
  
  private static final String UPDATE_TEAM_OF_PLAYER_DEFINE_NULL = "UPDATE player SET player_fk_team = %s WHERE player_id = %d";
  
  private PlayerRepository() {
    //hide default public constructor
  }
  
  public static void createPlayer(long servId, long discordId, boolean mentionnable) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(INSERT_INTO_PLAYER, servId, discordId, mentionnable);
      query.execute(finalQuery);
    }
  }
  
  public static void updateTeamOfPlayer(long playerId, long teamId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(UPDATE_TEAM_OF_PLAYER, teamId, playerId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static void updateTeamOfPlayerDefineNull(long playerId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(UPDATE_TEAM_OF_PLAYER_DEFINE_NULL, "NULL", playerId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static void deletePlayer(long playerId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(DELETE_PLAYER_WITH_PLAYER_ID, playerId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static List<DTO.Player> getPlayers(long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_PLAYERS_WITH_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);
      
      
      List<DTO.Player> accounts = new ArrayList<>();
      result.next();
      while(!result.isAfterLast()) {
        accounts.add(new DTO.Player(result));
        result.next();
      }
      
      return accounts;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static DTO.Player getPlayer(long guildId, long playerDiscordId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_PLAYER_WITH_DISCORD_ID, guildId, playerDiscordId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.Player(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  @Nullable
  public static DTO.Player getPlayerByLeagueAccountAndGuild(long guildId, String summonerId, String server) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_PLAYER_WITH_GUILD_ID_AND_LEAGUE_ACCOUNT, guildId, summonerId, server);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.Player(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
 
  
}
