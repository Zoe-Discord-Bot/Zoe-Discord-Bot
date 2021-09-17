package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class LeagueAccountRepository {

  private static final String INSERT_LEAGUE_ACCOUNT = "INSERT INTO league_account " +
      "(leagueaccount_fk_player, leagueaccount_name, " +
      "leagueaccount_summonerid, leagueaccount_accountid, leagueaccount_puuid, leagueaccount_server, leagueAccount_tftSummonerId, leagueAccount_tftAccountId, leagueAccount_tftPuuid) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String SELECT_LEAGUES_ACCOUNTS_WITH_GUILDID_AND_PLAYER_DISCORD_ID =
      "SELECT " + 
          "league_account.leagueaccount_id, " + 
          "league_account.leagueaccount_fk_player, " + 
          "league_account.leagueaccount_fk_gamecard, " + 
          "league_account.leagueaccount_fk_currentgame, " +
          "league_account.leagueaccount_name, " + 
          "league_account.leagueaccount_summonerid, " + 
          "league_account.leagueaccount_accountid, " +
          "league_account.leagueaccount_puuid, " +
          "league_account.leagueAccount_tftSummonerId, " +
          "league_account.leagueAccount_tftAccountId, " +
          "league_account.leagueAccount_tftPuuid, " +
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
          "league_account.leagueaccount_name, " + 
          "league_account.leagueaccount_summonerid, " + 
          "league_account.leagueaccount_accountid, " + 
          "league_account.leagueaccount_puuid, " + 
          "league_account.leagueAccount_tftSummonerId, " +
          "league_account.leagueAccount_tftAccountId, " +
          "league_account.leagueAccount_tftPuuid, " +
          "league_account.leagueaccount_server " + 
          "FROM game_info_card " + 
          "INNER JOIN league_account ON game_info_card.gamecard_id = league_account.leagueaccount_fk_gamecard " + 
          "WHERE game_info_card.gamecard_id = %d";
  
  private static final String SELECT_LEAGUE_ACCOUNT_WITH_LEAGUEACCOUNT_ID =
      "SELECT " + 
          "league_account.leagueaccount_id, " + 
          "league_account.leagueaccount_fk_player, " + 
          "league_account.leagueaccount_fk_gamecard, " +
          "league_account.leagueaccount_fk_currentgame, " +
          "league_account.leagueaccount_name, " + 
          "league_account.leagueaccount_summonerid, " + 
          "league_account.leagueaccount_accountid, " + 
          "league_account.leagueaccount_puuid, " + 
          "league_account.leagueAccount_tftSummonerId, " +
          "league_account.leagueAccount_tftAccountId, " +
          "league_account.leagueAccount_tftPuuid, " +
          "league_account.leagueaccount_server " + 
          "FROM game_info_card " + 
          "WHERE league_account.leagueaccount_id = %d";

  private static final String SELECT_ALL_LEAGUES_ACCOUNTS_WITH_GUILD_ID =
      "SELECT " + 
          "league_account.leagueaccount_id, " + 
          "league_account.leagueaccount_fk_player, " + 
          "league_account.leagueaccount_fk_gamecard, " + 
          "league_account.leagueaccount_fk_currentgame, " +
          "league_account.leagueaccount_name, " + 
          "league_account.leagueaccount_summonerid, " + 
          "league_account.leagueaccount_accountid, " + 
          "league_account.leagueaccount_puuid, " + 
          "league_account.leagueAccount_tftSummonerId, " +
          "league_account.leagueAccount_tftAccountId, " +
          "league_account.leagueAccount_tftPuuid, " +
          "league_account.leagueaccount_server " + 
          "FROM league_account " + 
          "INNER JOIN player ON league_account.leagueaccount_fk_player = player.player_id " + 
          "INNER JOIN server ON player.player_fk_server = server.serv_id " + 
          "WHERE server.serv_guildid = %d";
  
  private static final String SELECT_LEAGUE_ACCOUNT_WITH_GUILD_ID_AND_SUMMONER_ID_AND_SERVER = "SELECT " + 
      "league_account.leagueaccount_id, " + 
      "league_account.leagueaccount_fk_player, " + 
      "league_account.leagueaccount_fk_gamecard, " + 
      "league_account.leagueaccount_fk_currentgame, " + 
      "league_account.leagueaccount_name, " + 
      "league_account.leagueaccount_summonerid, " + 
      "league_account.leagueaccount_accountid, " + 
      "league_account.leagueaccount_puuid, " + 
      "league_account.leagueAccount_tftSummonerId, " +
      "league_account.leagueAccount_tftAccountId, " +
      "league_account.leagueAccount_tftPuuid, " +
      "league_account.leagueaccount_server " + 
      "FROM server " + 
      "INNER JOIN player ON server.serv_id = player.player_fk_server " + 
      "INNER JOIN league_account ON player.player_id = league_account.leagueaccount_fk_player " + 
      "WHERE server.serv_guildid = %d " + 
      "AND league_account.leagueaccount_summonerid = '%s' " + 
      "AND league_account.leagueaccount_server = '%s'";

  private static final String DELETE_LEAGUE_ACCOUNT_WITH_ID = "DELETE FROM league_account WHERE leagueaccount_id = %d";
  
  private static final String SELECT_LEAGUES_ACCOUNTS_WITH_CURRENT_GAME_ID =
      "SELECT " + 
          "league_account.leagueaccount_id, " + 
          "league_account.leagueaccount_fk_player, " + 
          "league_account.leagueaccount_fk_gamecard, " + 
          "league_account.leagueaccount_fk_currentgame, " + 
          "league_account.leagueaccount_name, " + 
          "league_account.leagueaccount_summonerid, " + 
          "league_account.leagueaccount_accountid, " + 
          "league_account.leagueaccount_puuid, " + 
          "league_account.leagueAccount_tftSummonerId, " +
          "league_account.leagueAccount_tftAccountId, " +
          "league_account.leagueAccount_tftPuuid, " +
          "league_account.leagueaccount_server " + 
          "FROM league_account " + 
          "WHERE league_account.leagueaccount_fk_currentgame = %d";
  
  private static final String SELECT_LEAGUES_ACCOUNTS_WITH_SUMMONER_ID_AND_SERVER = "SELECT " + 
      "league_account.leagueaccount_id, " + 
      "league_account.leagueaccount_fk_player, " + 
      "league_account.leagueaccount_fk_gamecard, " + 
      "league_account.leagueaccount_fk_currentgame, " + 
      "league_account.leagueaccount_name, " + 
      "league_account.leagueaccount_summonerid, " + 
      "league_account.leagueaccount_accountid, " + 
      "league_account.leagueaccount_puuid, " + 
      "league_account.leagueAccount_tftSummonerId, " +
      "league_account.leagueAccount_tftAccountId, " +
      "league_account.leagueAccount_tftPuuid, " +
      "league_account.leagueaccount_server " + 
      "FROM server " + 
      "INNER JOIN player ON server.serv_id = player.player_fk_server " + 
      "INNER JOIN league_account ON player.player_id = league_account.leagueaccount_fk_player " +  
      "WHERE league_account.leagueaccount_summonerid = '%s' " + 
      "AND league_account.leagueaccount_server = '%s'";

  private static final String SELECT_LEAGUES_ACCOUNTS_WITH_PLAYER_ID_AND_GUILD_ID =
      "SELECT " + 
          "league_account.leagueaccount_id, " + 
          "league_account.leagueaccount_fk_player, " + 
          "league_account.leagueaccount_fk_gamecard, " + 
          "league_account.leagueaccount_fk_currentgame, " + 
          "league_account.leagueaccount_name, " + 
          "league_account.leagueaccount_summonerid, " + 
          "league_account.leagueaccount_accountid, " + 
          "league_account.leagueaccount_puuid, " + 
          "league_account.leagueAccount_tftSummonerId, " +
          "league_account.leagueAccount_tftAccountId, " +
          "league_account.leagueAccount_tftPuuid, " +
          "league_account.leagueaccount_server " + 
          "FROM server " + 
          "INNER JOIN player ON server.serv_id = player.player_fk_server " + 
          "INNER JOIN league_account ON player.player_id = league_account.leagueaccount_fk_player " + 
          "WHERE player.player_id = %d " + 
          "AND server.serv_guildid = %d";

  private static final String UPDATE_LEAGUE_ACCOUNT_CURRENT_GAME_WITH_ID = 
      "UPDATE league_account SET leagueaccount_fk_currentgame = %s WHERE leagueaccount_id = %d";

  private static final String UPDATE_LEAGUE_ACCOUNT_GAME_CARD_WITH_ID = 
      "UPDATE league_account SET leagueaccount_fk_gamecard = %s WHERE leagueaccount_id = %d";

  private static final String UPDATE_LEAGUE_ACCOUNT_NAME_WITH_ID =
      "UPDATE league_account SET leagueaccount_name = '%s' WHERE leagueaccount_id = %d";
  
  private static final String UPDATE_LEAGUE_ACCOUNT_TFT_DATA_WITH_ID = 
      "UPDATE league_account SET leagueAccount_tftSummonerId = '%s', leagueAccount_tftAccountId = '%s', leagueAccount_tftPuuid = '%s' WHERE leagueaccount_id = %d";

  private static final String COUNT_LEAGUE_ACCOUNTS = "SELECT COUNT(*) FROM league_account";

  private static final String UPDATE_LEAGUE_ACCOUNT_DATA_WITH_ID =
      "UPDATE league_account SET leagueAccount_summonerId = '%s', leagueAccount_accountId = '%s', leagueAccount_puuid = '%s', leagueaccount_server = '%s' WHERE leagueaccount_id = %d";

  private LeagueAccountRepository() {
    //hide default public constructor
  }

  public static long countLeagueAccounts() throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      result = query.executeQuery(COUNT_LEAGUE_ACCOUNTS);

      result.first();

      return result.getLong("count");
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static void updateAccountTFTDataWithId(long leagueAccountId, Summoner summoner) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_TFT_DATA_WITH_ID, summoner.getSummonerId(), summoner.getAccountId(), summoner.getPUUID(), leagueAccountId);
      query.executeUpdate(finalQuery);
    }
  }

  public static List<DTO.LeagueAccount> getLeaguesAccountsWithPlayerID(long guildId, long playerId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      return getLeaguesAccountsWithPlayerID(guildId, playerId, conn);
    }
  }
  
  public static List<DTO.LeagueAccount> getLeaguesAccountsWithPlayerID(long guildId, long playerId, Connection conn) throws SQLException {
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_LEAGUES_ACCOUNTS_WITH_PLAYER_ID_AND_GUILD_ID, playerId, guildId);
      result = query.executeQuery(finalQuery);
      List<DTO.LeagueAccount> accounts = Collections.synchronizedList(new ArrayList<>());
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return accounts;
      }
      result.first();
      while(!result.isAfterLast()) {
        accounts.add(new DTO.LeagueAccount(result));
        result.next();
      }

      return accounts;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.LeagueAccount> getLeaguesAccountsWithCurrentGameId(long currentGameId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      return getLeaguesAccountsWithCurrentGameId(currentGameId, conn);
    }
  }

  public static List<DTO.LeagueAccount> getLeaguesAccountsWithCurrentGameId(long currentGameId, Connection conn) throws SQLException {
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_LEAGUES_ACCOUNTS_WITH_CURRENT_GAME_ID, currentGameId);
      result = query.executeQuery(finalQuery);

      List<DTO.LeagueAccount> accounts = Collections.synchronizedList(new ArrayList<>());
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return accounts;
      }
      result.first();
      while(!result.isAfterLast()) {
        accounts.add(new DTO.LeagueAccount(result));
        result.next();
      }

      return accounts;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static List<DTO.LeagueAccount> getLeaguesAccountsWithGameCardsId(long gameCardsId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      return getLeaguesAccountsWithGameCardsId(gameCardsId, conn);
    }
  }
  
  public static List<DTO.LeagueAccount> getLeaguesAccountsWithGameCardsId(long gameCardsId, Connection conn) throws SQLException {
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_LEAGUES_ACCOUNTS_WITH_GAME_INFO_CARD_ID, gameCardsId);
      result = query.executeQuery(finalQuery);

      List<DTO.LeagueAccount> accounts = Collections.synchronizedList(new ArrayList<>());
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return accounts;
      }
      result.first();
      while(!result.isAfterLast()) {
        accounts.add(new DTO.LeagueAccount(result));
        result.next();
      }

      return accounts;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static void createLeagueAccount(long playerId, SavedSummoner summoner, SavedSummoner tftSummoner, ZoePlatform server) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        PreparedStatement createQuery = conn.prepareStatement(INSERT_LEAGUE_ACCOUNT)) {

      createQuery.setLong(1, playerId);
      createQuery.setString(2, summoner.getName());
      createQuery.setString(3, summoner.getSummonerId());
      createQuery.setString(4, summoner.getAccountId());
      createQuery.setString(5, summoner.getPuuid());
      createQuery.setString(6, server.getDbName());
      createQuery.setString(7, tftSummoner.getSummonerId());
      createQuery.setString(8, tftSummoner.getAccountId());
      createQuery.setString(9, tftSummoner.getPuuid());
      
      createQuery.executeUpdate();
    }
  }

  public static List<DTO.LeagueAccount> getLeaguesAccounts(long guildId, long discordPlayerId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_LEAGUES_ACCOUNTS_WITH_GUILDID_AND_PLAYER_DISCORD_ID, guildId, discordPlayerId);
      result = query.executeQuery(finalQuery);

      List<DTO.LeagueAccount> accounts = Collections.synchronizedList(new ArrayList<>());
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return accounts;
      }
      result.first();
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
    try (Connection conn = RepoRessources.getConnection();) {
      return getAllLeaguesAccounts(guildId, conn);
    }
  }
  
  public static List<DTO.LeagueAccount> getAllLeaguesAccounts(long guildId, Connection conn) throws SQLException {
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_ALL_LEAGUES_ACCOUNTS_WITH_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);

      List<DTO.LeagueAccount> accounts = Collections.synchronizedList(new ArrayList<>());
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return accounts;
      }
      result.first();
      while(!result.isAfterLast()) {
        accounts.add(new DTO.LeagueAccount(result));
        result.next();
      }

      return accounts;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.LeagueAccount> getLeaguesAccountsWithSummonerIdAndServer(String summonerId, ZoePlatform server)
      throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_LEAGUES_ACCOUNTS_WITH_SUMMONER_ID_AND_SERVER, summonerId, server.getDbName());
      result = query.executeQuery(finalQuery);

      List<DTO.LeagueAccount> accounts = Collections.synchronizedList(new ArrayList<>());
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return accounts;
      }
      result.first();
      while(!result.isAfterLast()) {
        accounts.add(new DTO.LeagueAccount(result));
        result.next();
      }

      return accounts;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static DTO.LeagueAccount getLeagueAccountWithSummonerId(long guildId, String summonerId, ZoePlatform region) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_LEAGUE_ACCOUNT_WITH_GUILD_ID_AND_SUMMONER_ID_AND_SERVER,
          guildId, summonerId, region.getDbName());
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.LeagueAccount(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static DTO.LeagueAccount getLeagueAccountWithLeagueAccountId(long leagueAccountId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_LEAGUE_ACCOUNT_WITH_LEAGUEACCOUNT_ID, leagueAccountId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.LeagueAccount(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static void updateAccountCurrentGameWithAccountId(long leagueAccountId, long currentGameId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      updateAccountCurrentGameWithAccountId(leagueAccountId, currentGameId, conn);
    }
  }

  public static void updateAccountCurrentGameWithAccountId(long leagueAccountId, long currentGameId, Connection conn) throws SQLException {
    try (Statement query = conn.createStatement();) {

      String finalQuery;
      if(currentGameId == 0) {
        finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_CURRENT_GAME_WITH_ID, "NULL", leagueAccountId);
      }else {
        finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_CURRENT_GAME_WITH_ID, Long.toString(currentGameId), leagueAccountId);
      }
      query.executeUpdate(finalQuery);
    }
  }

  /**
   * We don't use this field anymore, please now use {@link SummonerCacheRepository}
   */
  @Deprecated
  public static void updateAccountNameWithAccountId(long leagueAccountId, String name) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_NAME_WITH_ID, name, leagueAccountId);
      query.executeUpdate(finalQuery);
    }
  }

  public static void updateAccountGameCardWithAccountId(long leagueAccountId, long gameCardId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      updateAccountGameCardWithAccountId(leagueAccountId, gameCardId, conn);
    }
  }
  
  public static void updateAccountGameCardWithAccountId(long leagueAccountId, long gameCardId, Connection conn) throws SQLException {
    try (Statement query = conn.createStatement();) {

      String finalQuery;
      if(gameCardId == 0) {
        finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_GAME_CARD_WITH_ID, "NULL", leagueAccountId);
      }else {
        finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_GAME_CARD_WITH_ID, Long.toString(gameCardId), leagueAccountId);
      }

      query.executeUpdate(finalQuery);
    }
  }
  
  public static void deleteAccountWithId(long leagueAccountId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      deleteAccountWithId(leagueAccountId, conn);
    }
  }

  public static void deleteAccountWithId(long leagueAccountId, Connection conn) throws SQLException {
    try (Statement query = conn.createStatement();) {

      DTO.LastRank lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccountId, conn);

      if(lastRank != null) {
        LastRankRepository.deleteLastRank(lastRank.lastRank_id, conn);
      }

      String finalQuery = String.format(DELETE_LEAGUE_ACCOUNT_WITH_ID, leagueAccountId);
      query.executeUpdate(finalQuery);
    }
  }

  public static void updateAccountDataWithId(long leagueAccountId, Summoner summoner) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_DATA_WITH_ID, summoner.getSummonerId(), summoner.getAccountId(),
          summoner.getPUUID(), ZoePlatform.getZoePlatformByLeagueShard(summoner.getPlatform()).getDbName(), leagueAccountId);
      query.executeUpdate(finalQuery);
    }
  }

}
