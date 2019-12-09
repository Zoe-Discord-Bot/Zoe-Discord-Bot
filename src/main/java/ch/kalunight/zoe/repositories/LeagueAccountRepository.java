package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.model.dto.DTO;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;

public class LeagueAccountRepository {

  private static final String INSERT_LEAGUE_ACCOUNT = "INSERT INTO league_account " +
      "(leagueaccount_fk_player, leagueaccount_summonerid, leagueaccount_accountid, leagueaccount_puuid, leagueaccount_server) " +
      "VALUES (%d, '%s', '%s', '%s', '%s')";
  
  private static final String SELECT_LEAGUES_ACCOUNTS_WITH_GUILDID_AND_PLAYER_DISCORD_ID =
      "SELECT " + 
      "league_account.leagueaccount_id, " + 
      "league_account.leagueaccount_fk_player, " + 
      "league_account.leagueaccount_fk_gamecard, " + 
      "league_account.leagueaccount_summonerid, " +
      "league_account.leagueaccount_accountid, " +
      "league_account.leagueaccount_puuid, " + 
      "league_account.leagueaccount_server, " + 
      "league_account.leagueaccount_currentgame " + 
      "FROM server " + 
      "INNER JOIN player ON server.serv_id = player.player_fk_server " + 
      "INNER JOIN league_account ON player.player_id = league_account.leagueaccount_fk_player " + 
      "WHERE server.serv_guildid = %d " + 
      "AND player.player_discordid = %d";
  
  private LeagueAccountRepository() {
    //hide default public constructor
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
      
      
      List<DTO.LeagueAccount> accounts = new ArrayList<>();
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
  
  
}
