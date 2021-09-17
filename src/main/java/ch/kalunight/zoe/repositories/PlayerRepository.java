package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.model.dto.DTO.Player;
import net.dv8tion.jda.api.entities.Guild;

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
  
  private static final String SELECT_ALL_PLAYERS_DISCORD_ID = "SELECT server.serv_guildid, player.player_discordid "
      + "FROM server "
      + "INNER JOIN player ON server.serv_id = player.player_fk_server";

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

  private static final String COUNT_PLAYERS = "SELECT COUNT(*) FROM player";
  
  private static final Logger logger = LoggerFactory.getLogger(PlayerRepository.class);
  
  /**
   * This list contain discordId of player registered by guildid. Sync with the DB<p>
   * First long : Guild id<p>
   * List of long : User's Discord Id
   */
  private static final Map<Long, List<Long>> LIST_DISCORD_ID_OF_REGISTERED_PLAYERS = Collections.synchronizedMap(new HashMap<>());
  
  private static final List<Long> LOADED_GUILD = Collections.synchronizedList(new ArrayList<>());
  
  private PlayerRepository() {
    //hide default public constructor
  }
  
  public static void setupListOfRegisteredPlayers() throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = SELECT_ALL_PLAYERS_DISCORD_ID;
      result = query.executeQuery(finalQuery);

      long numberOfPlayers = 0;
      
      LIST_DISCORD_ID_OF_REGISTERED_PLAYERS.clear();
      if(0 != (result.last() ? result.getRow() : 0)) {
        result.first();
        while(!result.isAfterLast()) {
          long serverGuildId = result.getLong("serv_guildid");
          long playerDiscordId = result.getLong("player_discordid");
          
          addADiscordIdToTrack(serverGuildId, playerDiscordId);
          result.next();
          
          numberOfPlayers++;
        }
      }
      
      logger.info("List of registered player initated ! Number of guilds : {} / Number Of players : {}", LIST_DISCORD_ID_OF_REGISTERED_PLAYERS.size(), numberOfPlayers);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  private static void addADiscordIdToTrack(long serverGuildId, long playerDiscordId) {
    List<Long> listOfRegisterdPlayersForThisGuild = LIST_DISCORD_ID_OF_REGISTERED_PLAYERS.get(serverGuildId);
    if(listOfRegisterdPlayersForThisGuild == null) {
      List<Long> newList = Collections.synchronizedList(new ArrayList<>());
      newList.add(playerDiscordId);
      LIST_DISCORD_ID_OF_REGISTERED_PLAYERS.put(serverGuildId, newList);
    }else {
      listOfRegisterdPlayersForThisGuild.add(playerDiscordId);
    }
    
    Guild guild = Zoe.getGuildById(serverGuildId);
    
    if(guild != null) {
      guild.retrieveMemberById(playerDiscordId).queue();
    }
  }
  
  public static long countPlayers() throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      result = query.executeQuery(COUNT_PLAYERS);
      
      result.first();
      
      return result.getLong("count");
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static void createPlayer(long servId, long serverGuildId, long discordId, boolean mentionnable) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(INSERT_INTO_PLAYER, servId, discordId, mentionnable);
      query.execute(finalQuery);
      addADiscordIdToTrack(serverGuildId, discordId);
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
    try (Connection conn = RepoRessources.getConnection();) {
      updateTeamOfPlayerDefineNull(playerId, conn);
    }
  }
  
  public static void updateTeamOfPlayerDefineNull(long playerId, Connection conn) throws SQLException {
    try (Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_TEAM_OF_PLAYER_DEFINE_NULL, "NULL", playerId);
      query.executeUpdate(finalQuery);
    }
  }

  public static void deletePlayer(Player player, long guildId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      deletePlayer(player, guildId, conn);
    }
  }
  
  public static void deletePlayer(Player player, long guildId, Connection conn) throws SQLException {
    try (Statement query = conn.createStatement();) {

      List<DTO.LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithPlayerID(guildId, player.player_id, conn);
      
      for(DTO.LeagueAccount leagueAccount : leaguesAccounts) {
        LeagueAccountRepository.deleteAccountWithId(leagueAccount.leagueAccount_id, conn);
      }
      
      String finalQuery = String.format(DELETE_PLAYER_WITH_PLAYER_ID, player.player_id);
      query.executeUpdate(finalQuery);
      List<Long> playersOfTheDiscord = LIST_DISCORD_ID_OF_REGISTERED_PLAYERS.get(guildId);
      if(playersOfTheDiscord != null) {
        playersOfTheDiscord.remove(player.player_discordId);
      }
      
      Guild guild = Zoe.getGuildById(guildId);
      
      if(guild != null) {
        guild.unloadMember(player.player_discordId);
      }
    }
  }

  public static List<DTO.Player> getPlayers(long guildId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      return getPlayers(guildId, conn);
    }
  }
  
  public static List<DTO.Player> getPlayers(long guildId, Connection conn) throws SQLException {
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_PLAYERS_WITH_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);

      List<DTO.Player> accounts = new ArrayList<>();
      if(0 != (result.last() ? result.getRow() : 0)) {
        result.first();
        while(!result.isAfterLast()) {
          accounts.add(new DTO.Player(result));
          result.next();
        }
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
  public static DTO.Player getPlayerByLeagueAccountAndGuild(long guildId, String summonerId, ZoePlatform server) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_PLAYER_WITH_GUILD_ID_AND_LEAGUE_ACCOUNT, guildId, summonerId, server.getDbName());
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

  public static Map<Long, List<Long>> getListDiscordIdOfRegisteredPlayers() {
    return LIST_DISCORD_ID_OF_REGISTERED_PLAYERS;
  }

  public static List<Long> getLoadedGuild() {
    return LOADED_GUILD;
  }
}
