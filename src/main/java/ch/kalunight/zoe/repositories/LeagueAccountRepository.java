package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class LeagueAccountRepository {

  private static final String INSERT_LEAGUE_ACCOUNT = "INSERT INTO league_account " +
      "(leagueaccount_fk_player, leagueaccount_summonerid, leagueaccount_accountid, leagueaccount_puuid, leagueaccount_server) " +
      "VALUES (%d, '%s', '%s', '%s', '%s')";
  
  private static final String SELECT_LEAGUES_ACCOUNTS_WITH_GUILDID_AND_PLAYER_DISCORD_ID =
      "SELECT " + 
      "league_account.leagueaccount_id, " + 
      "league_account.leagueaccount_fk_player, " + 
      "league_account.leagueaccount_fk_gamecard, " + 
      "league_account.leagueaccount_fk_currentgame, " +
      "league_account.leagueaccount_summonerid, " +
      "league_account.leagueaccount_accountid, " +
      "league_account.leagueaccount_puuid, " + 
      "league_account.leagueaccount_server " +
      "FROM server " + 
      "INNER JOIN player ON server.serv_id = player.player_fk_server " + 
      "INNER JOIN league_account ON player.player_id = league_account.leagueaccount_fk_player " + 
      "WHERE server.serv_guildid = %d " + 
      "AND player.player_discordid = %d";
  
  private static final String SELECT_LEAGUES_ACCOUNTS_WITH_GAME_INFO_CARD_ID = 
      "SELECT " + 
      "league_account.leagueaccount_id, " + 
      "league_account.leagueaccount_fk_player, " + 
      "league_account.leagueaccount_fk_gamecard, " +
      "league_account.leagueaccount_fk_currentgame, " +
      "league_account.leagueaccount_summonerid, " + 
      "league_account.leagueaccount_accountid, " + 
      "league_account.leagueaccount_puuid, " + 
      "league_account.leagueaccount_server " + 
      "FROM game_info_card " + 
      "INNER JOIN league_account ON game_info_card.gamecard_id = league_account.leagueaccount_fk_gamecard " + 
      "WHERE game_info_card.gamecard_id = %d";
  
  private static final String SELECT_ALL_LEAGUES_ACCOUNTS_WITH_GUILD_ID =
      "SELECT " + 
      "league_account.leagueaccount_id, " + 
      "league_account.leagueaccount_fk_player, " + 
      "league_account.leagueaccount_fk_gamecard, " + 
      "league_account.leagueaccount_fk_currentgame, " + 
      "league_account.leagueaccount_summonerid, " + 
      "league_account.leagueaccount_accountid, " + 
      "league_account.leagueaccount_puuid, " + 
      "league_account.leagueaccount_server " + 
      "FROM league_account " + 
      "INNER JOIN player ON league_account.leagueaccount_fk_player = player.player_id " + 
      "INNER JOIN server ON player.player_fk_server = server.serv_id " + 
      "WHERE server.serv_guildid = %d";
  
  private static final String DELETE_LEAGUE_ACCOUNT_WITH_ID = "DELETE FROM league_account WHERE leagueaccount_id = %d";
  
  private static final String UPDATE_LEAGUE_ACCOUNT_CURRENT_GAME_WITH_ID = 
      "UPDATE league_account SET leagueaccount_fk_currentgame  = %d WHERE leagueaccount_id = %d";
  
  private LeagueAccountRepository() {
    //hide default public constructor
  }
  
  public static List<DTO.LeagueAccount> getLeaguesAccountsWithGameCardsId(long gameCardsId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_LEAGUES_ACCOUNTS_WITH_GAME_INFO_CARD_ID, gameCardsId);
      result = query.executeQuery(finalQuery);
      
      List<DTO.LeagueAccount> accounts = Collections.synchronizedList(new ArrayList<>());
      result.next();
      while(!result.isAfterLast()) {
        accounts.add(new DTO.LeagueAccount(result));
        result.next();
      }
      
      return accounts;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static void createLeagueAccount(long playerId, Summoner summoner, String server) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(INSERT_LEAGUE_ACCOUNT, playerId, summoner.getId(),
          summoner.getAccountId(), summoner.getPuuid(), server);
      query.execute(finalQuery);
    }
  }
  
  public static List<DTO.LeagueAccount> getLeaguesAccounts(long guildId, long discordPlayerId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_LEAGUES_ACCOUNTS_WITH_GUILDID_AND_PLAYER_DISCORD_ID, guildId, discordPlayerId);
      result = query.executeQuery(finalQuery);
      
      List<DTO.LeagueAccount> accounts = Collections.synchronizedList(new ArrayList<>());
      result.next();
      while(!result.isAfterLast()) {
        accounts.add(new DTO.LeagueAccount(result));
        result.next();
      }
      
      return accounts;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.LeagueAccount> getAllLeaguesAccounts(long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_ALL_LEAGUES_ACCOUNTS_WITH_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);
      
      List<DTO.LeagueAccount> accounts = Collections.synchronizedList(new ArrayList<>());
      result.next();
      while(!result.isAfterLast()) {
        accounts.add(new DTO.LeagueAccount(result));
        result.next();
      }
      
      return accounts;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static DTO.LeagueAccount getLeagueAccountByName(long guildId, long discordPlayerId,
      String summonerName, Platform region) throws SQLException, RiotApiException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();) {
      
      for(DTO.LeagueAccount account : getLeaguesAccounts(guildId, discordPlayerId)) {
        if(account.leagueAccount_server.equals(region)) {
          Summoner summoner = Zoe.getRiotApi().getSummoner(region, account.leagueAccount_summonerId, CallPriority.HIGH);
          if(summoner.getName().equals(summonerName)) {
            return account;
          }
        }
      }
      return null;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static void updateAccountCurrentGameWithAccountId(long leagueAccountId, long currentGameId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_CURRENT_GAME_WITH_ID, currentGameId, leagueAccountId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static void deleteAccountWithId(long leagueAccountId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(DELETE_LEAGUE_ACCOUNT_WITH_ID, leagueAccountId);
      query.executeUpdate(finalQuery);
    }
  }
  
}
