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
import net.rithms.riot.api.endpoints.tft_summoner.dto.TFTSummoner;
import net.rithms.riot.constant.Platform;

public class LeagueAccountRepository {

  private static final String INSERT_LEAGUE_ACCOUNT = "INSERT INTO league_account " +
      "(leagueaccount_fk_player, leagueaccount_name, " +
      "leagueaccount_summonerid, leagueaccount_accountid, leagueaccount_puuid, leagueaccount_server, leagueAccount_tftSummonerId, leagueAccount_tftAccountId, leagueAccount_tftPuuid) " +
      "VALUES (%d, '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')";

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

  public static void updateAccountTFTDataWithId(long leagueAccountId, TFTSummoner summoner) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_TFT_DATA_WITH_ID, summoner.getId(), summoner.getAccountId(), summoner.getPuuid(), leagueAccountId);
      query.executeUpdate(finalQuery);
    }
  }

  public static List<DTO.LeagueAccount> getLeaguesAccountsWithPlayerID(long guildId, long playerId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

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
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

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
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

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

  public static void createLeagueAccount(long playerId, Summoner summoner, TFTSummoner tftSummoner, String server) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(INSERT_LEAGUE_ACCOUNT, playerId, summoner.getName(), summoner.getId(),
          summoner.getAccountId(), summoner.getPuuid(), server, tftSummoner.getId(), tftSummoner.getAccountId(), tftSummoner.getPuuid());
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
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

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
  
  public static List<DTO.LeagueAccount> getLeaguesAccountsWithSummonerIdAndServer(String summonerId, Platform server)
      throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_LEAGUES_ACCOUNTS_WITH_SUMMONER_ID_AND_SERVER, summonerId, server.getName());
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
  
  public static DTO.LeagueAccount getLeagueAccountByName(long guildId, long discordPlayerId,
      String summonerName, Platform region) throws SQLException, RiotApiException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();) {

      for(DTO.LeagueAccount account : getLeaguesAccounts(guildId, discordPlayerId)) {
        if(account.leagueAccount_server.equals(region)) {
          Summoner summoner = Zoe.getRiotApi().getSummoner(region, account.leagueAccount_summonerId);
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

  public static DTO.LeagueAccount getLeagueAccountWithSummonerId(long guildId, String summonerId, Platform region) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_LEAGUE_ACCOUNT_WITH_GUILD_ID_AND_SUMMONER_ID_AND_SERVER,
          guildId, summonerId, region.getName());
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
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery;
      if(currentGameId == 0) {
        finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_CURRENT_GAME_WITH_ID, "NULL", leagueAccountId);
      }else {
        finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_CURRENT_GAME_WITH_ID, Long.toString(currentGameId), leagueAccountId);
      }
      query.executeUpdate(finalQuery);
    }
  }

  public static void updateAccountNameWithAccountId(long leagueAccountId, String name) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_LEAGUE_ACCOUNT_NAME_WITH_ID, name, leagueAccountId);
      query.executeUpdate(finalQuery);
    }
  }

  public static void updateAccountGameCardWithAccountId(long leagueAccountId, long gameCardId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

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
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      DTO.LastRank lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccountId);

      if(lastRank != null) {
        LastRankRepository.deleteLastRank(lastRank.lastRank_id);
      }

      String finalQuery = String.format(DELETE_LEAGUE_ACCOUNT_WITH_ID, leagueAccountId);
      query.executeUpdate(finalQuery);
    }
  }

}
