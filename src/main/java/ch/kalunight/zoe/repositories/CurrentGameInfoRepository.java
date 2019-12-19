package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ch.kalunight.zoe.model.dto.DTO;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class CurrentGameInfoRepository {

  private static final String SELECT_CURRENT_GAME_INFO_WITH_LEAGUE_ACCOUNT_ID = 
      "SELECT " + 
          "current_game_info.currentgame_id,current_game_info.currentgame_currentgame " + 
          "FROM current_game_info " + 
          "INNER JOIN league_account ON current_game_info.currentgame_id = league_account.leagueaccount_fk_currentgame " + 
          "WHERE league_account.leagueaccount_id = %d";

  private static final String INSERT_CURRENT_GAME = "INSERT INTO current_game_info " +
      "(currentgame_currentgame) " +
      "VALUES ('%s') RETURNING currentgame_id";

  private static final String UPDATE_CURRENT_GAME_WITH_ID = 
      "UPDATE current_game_info SET currentgame_currentgame = '%s' WHERE currentgame_id = %d";

  private static final String DELETE_CURRENT_GAME_WITH_ID = "DELETE FROM current_game_info WHERE currentgame_id = %d";

  private static final Gson gson = new GsonBuilder().create();

  private CurrentGameInfoRepository() {
    //hide default public constructor
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

  public static void createCurrentGame(CurrentGameInfo currentGame, DTO.LeagueAccount leagueAccount) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String currentGameJson = gson.toJson(currentGame);

      String finalQuery = String.format(INSERT_CURRENT_GAME, currentGameJson);
      result = query.executeQuery(finalQuery);
      result.next();

      LeagueAccountRepository.updateAccountCurrentGameWithAccountId(leagueAccount.leagueAccount_id, result.getLong("currentgame_id"));
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
        String finalQuery = String.format(UPDATE_CURRENT_GAME_WITH_ID, currentGameJson, currentGameInfo.currentgame_id);
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

}
