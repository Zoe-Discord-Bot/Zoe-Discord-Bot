package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.constant.Platform;

public class CurrentGameInfoRepository {

  private static final String SELECT_CURRENT_GAME_INFO_WITH_LEAGUE_ACCOUNT_ID = 
      "SELECT " + 
          "current_game_info.currentgame_id, " + 
          "current_game_info.currentgame_currentgame, " + 
          "current_game_info.currentgame_server, " + 
          "current_game_info.currentgame_gameid " + 
          "FROM current_game_info " + 
          "INNER JOIN league_account ON current_game_info.currentgame_id = league_account.leagueaccount_fk_currentgame " + 
          "WHERE league_account.leagueaccount_id = %d";

  private static final String SELECT_CURRENT_GAME_INFO_WITH_SERVER_AND_GAME_ID = 
      "SELECT " + 
      "current_game_info.currentgame_id, " + 
      "current_game_info.currentgame_currentgame, " + 
      "current_game_info.currentgame_server, " + 
      "current_game_info.currentgame_gameid " + 
      "FROM game_info_card " + 
      "INNER JOIN current_game_info ON game_info_card.gamecard_fk_currentgame = current_game_info.currentgame_id " + 
      "INNER JOIN league_account ON current_game_info.currentgame_id = league_account.leagueaccount_fk_currentgame " + 
      "INNER JOIN player ON league_account.leagueaccount_fk_player = player.player_id " + 
      "INNER JOIN server ON player.player_fk_server = server.serv_id " + 
      "INNER JOIN info_channel ON server.serv_id = info_channel.infochannel_fk_server " + 
      "INNER JOIN league_account AS league_account_1 ON game_info_card.gamecard_id = league_account_1.leagueaccount_fk_gamecard " + 
      "INNER JOIN info_channel AS info_channel_1 ON game_info_card.gamecard_fk_infochannel = info_channel_1.infochannel_id " + 
      "WHERE current_game_info.currentgame_server = '%s' " + 
      "AND current_game_info.currentgame_gameid = '%s' " + 
      "AND server.serv_guildid = %s";

  private static final String SELECT_ALL_CURRENT_GAME =
      "SELECT " + 
          "current_game_info.currentgame_id, " + 
          "current_game_info.currentgame_currentgame, " + 
          "current_game_info.currentgame_server, " + 
          "current_game_info.currentgame_gameid " + 
          "FROM current_game_info";
  
  private static final String SELECT_CURRENT_GAME_WITHOUT_GAME_INFO_CARD_WITH_GUILD_ID =
      "SELECT " + 
          "current_game_info.currentgame_id, " + 
          "current_game_info.currentgame_currentgame, " + 
          "current_game_info.currentgame_server, " + 
          "current_game_info.currentgame_gameid " + 
          "FROM game_info_card " + 
          "RIGHT JOIN current_game_info ON game_info_card.gamecard_fk_currentgame = current_game_info.currentgame_id " + 
          "INNER JOIN league_account ON current_game_info.currentgame_id = league_account.leagueaccount_fk_currentgame " + 
          "INNER JOIN player ON league_account.leagueaccount_fk_player = player.player_id " + 
          "INNER JOIN server ON player.player_fk_server = server.serv_id " + 
          "WHERE game_info_card.gamecard_fk_currentgame IS NULL " + 
          "AND server.serv_guildid = %d";

  private static final String SELECT_CURRENT_GAME_WITHOUT_ACCOUNT_LINKED =
      "SELECT " + 
          "current_game_info.currentgame_id, " + 
          "current_game_info.currentgame_currentgame, " + 
          "current_game_info.currentgame_server, " + 
          "current_game_info.currentgame_gameid " + 
          "FROM game_info_card " + 
          "INNER JOIN current_game_info ON game_info_card.gamecard_fk_currentgame = current_game_info.currentgame_id " + 
          "LEFT JOIN league_account ON current_game_info.currentgame_id = league_account.leagueaccount_fk_currentgame " + 
          "INNER JOIN info_channel ON game_info_card.gamecard_fk_infochannel = info_channel.infochannel_id " + 
          "INNER JOIN server ON info_channel.infochannel_fk_server = server.serv_id " + 
          "WHERE server.serv_guildid = %d " + 
          "AND league_account.leagueaccount_id IS NULL";

  private static final String INSERT_CURRENT_GAME = "INSERT INTO current_game_info " +
      "(currentgame_currentgame, currentgame_gameid, currentgame_server) " +
      "VALUES ('%s', '%s', '%s') RETURNING currentgame_id";

  private static final String UPDATE_CURRENT_GAME_WITH_ID = 
      "UPDATE current_game_info SET currentgame_currentgame = '%s', currentgame_server = '%s', "
      + "currentgame_gameid = '%s' WHERE currentgame_id = %d";

  private static final String DELETE_CURRENT_GAME_WITH_ID = "DELETE FROM current_game_info WHERE currentgame_id = %d";

  private static final Gson gson = new GsonBuilder().create();

  private CurrentGameInfoRepository() {
    //hide default public constructor
  }
  
  public static List<DTO.CurrentGameInfo> getAllCurrentGameInfo() throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_ALL_CURRENT_GAME);
      result = query.executeQuery(finalQuery);

      List<DTO.CurrentGameInfo> gameCards = new ArrayList<>();
      if(0 != (result.last() ? result.getRow() : 0)) {
        result.first();
        while(!result.isAfterLast()) {
          gameCards.add(new DTO.CurrentGameInfo(result));
          result.next();
        }
      }

      return gameCards;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static List<DTO.CurrentGameInfo> getCurrentGameWithoutLinkWithGameCardAndWithGuildId(long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_CURRENT_GAME_WITHOUT_GAME_INFO_CARD_WITH_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);

      List<DTO.CurrentGameInfo> gameCards = new ArrayList<>();
      if(0 != (result.last() ? result.getRow() : 0)) {
        result.first();
        while(!result.isAfterLast()) {
          gameCards.add(new DTO.CurrentGameInfo(result));
          result.next();
        }
      }

      return gameCards;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  @Nullable
  public static DTO.CurrentGameInfo getCurrentGameWithLeagueAccountID(long leagueAccountId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_CURRENT_GAME_INFO_WITH_LEAGUE_ACCOUNT_ID, leagueAccountId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.CurrentGameInfo(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  @Nullable
  public static DTO.CurrentGameInfo getCurrentGameWithServerAndGameId(Platform platform, String gameID, Server server) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_CURRENT_GAME_INFO_WITH_SERVER_AND_GAME_ID, platform.getName(), gameID, server.serv_guildId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.CurrentGameInfo(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static long createCurrentGame(CurrentGameInfo currentGame, DTO.LeagueAccount leagueAccount) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String currentGameJson = gson.toJson(currentGame);

      String finalQuery = String.format(INSERT_CURRENT_GAME, currentGameJson, 
          Long.toString(currentGame.getGameId()), leagueAccount.leagueAccount_server.getName());
      result = query.executeQuery(finalQuery);
      result.next();

      long currentGameId = result.getLong("currentgame_id");
      
      LeagueAccountRepository.updateAccountCurrentGameWithAccountId(leagueAccount.leagueAccount_id, currentGameId);
      return currentGameId;
    } finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static void updateCurrentGame(CurrentGameInfo currentGame, DTO.LeagueAccount leagueAccount) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String currentGameJson = gson.toJson(currentGame);

      DTO.CurrentGameInfo currentGameInfo = getCurrentGameWithLeagueAccountID(leagueAccount.leagueAccount_id);

      if(currentGameInfo != null) {
        String finalQuery = String.format(UPDATE_CURRENT_GAME_WITH_ID, currentGameJson,
            leagueAccount.leagueAccount_server, currentGame.getGameId(), currentGameInfo.currentgame_id);
        query.execute(finalQuery);
      }
    }
  }
  
  public static void deleteCurrentGame(DTO.CurrentGameInfo currentGameDb, DTO.Server server) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      List<DTO.LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithCurrentGameId(currentGameDb.currentgame_id);

      for(DTO.LeagueAccount leagueAccount: leaguesAccounts) {
        LeagueAccountRepository.updateAccountCurrentGameWithAccountId(leagueAccount.leagueAccount_id, 0);
      }

      DTO.GameInfoCard gameInfoCard = GameInfoCardRepository.getGameInfoCardsWithCurrentGameId(server.serv_guildId, currentGameDb.currentgame_id);

      if(gameInfoCard != null) {
        GameInfoCardRepository.updateGameInfoCardsCurrentGamesWithId(0, gameInfoCard.gamecard_id);
      }
      String finalQuery = String.format(DELETE_CURRENT_GAME_WITH_ID, currentGameDb.currentgame_id);
      query.execute(finalQuery);
    }
  }


  public static List<DTO.CurrentGameInfo> getCurrentGamesWithoutLinkAccounts(long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_CURRENT_GAME_WITHOUT_ACCOUNT_LINKED, guildId);
      result = query.executeQuery(finalQuery);

      List<DTO.CurrentGameInfo> gameCards = new ArrayList<>();
      if(0 != (result.last() ? result.getRow() : 0)) {
        result.first();
        while(!result.isAfterLast()) {
          gameCards.add(new DTO.CurrentGameInfo(result));
          result.next();
        }
      }

      return gameCards;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

}
