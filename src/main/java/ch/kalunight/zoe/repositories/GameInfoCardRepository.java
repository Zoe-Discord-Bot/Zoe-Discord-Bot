package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.GameInfoCardStatus;

public class GameInfoCardRepository {

  private static final String SELECT_GAME_INFO_CARD_WITH_GUILD_ID =
      "SELECT " + 
          "game_info_card.gamecard_id, " + 
          "game_info_card.gamecard_fk_infochannel, " +
          "game_info_card.gamecard_fk_currentgame, " +
          "game_info_card.gamecard_titlemessageid, " + 
          "game_info_card.gamecard_infocardmessageid, " + 
          "game_info_card.gamecard_creationtime, " +
          "game_info_card.gamecard_status " + 
          "FROM game_info_card " + 
          "INNER JOIN info_channel ON game_info_card.gamecard_fk_infochannel = info_channel.infochannel_id " + 
          "INNER JOIN server ON info_channel.infochannel_fk_server = server.serv_id " + 
          "WHERE server.serv_guildid = %d";

  private static final String SELECT_GAME_INFO_CARD_WITH_GUILD_ID_CURRENT_GAME_INFO_ID_ACCOUNT =
      "SELECT " + 
          "game_info_card.gamecard_fk_infochannel, " + 
          "game_info_card.gamecard_fk_currentgame, " + 
          "game_info_card.gamecard_infocardmessageid, " + 
          "game_info_card.gamecard_titlemessageid, " + 
          "game_info_card.gamecard_creationtime, " + 
          "game_info_card.gamecard_id, " +
          "game_info_card.gamecard_status " +
          "FROM game_info_card " + 
          "INNER JOIN current_game_info ON game_info_card.gamecard_fk_currentgame = current_game_info.currentgame_id " + 
          "INNER JOIN league_account ON current_game_info.currentgame_id = league_account.leagueaccount_fk_currentgame " + 
          "INNER JOIN league_account AS league_account_1 ON game_info_card.gamecard_id = league_account_1.leagueaccount_fk_gamecard " + 
          "INNER JOIN info_channel ON game_info_card.gamecard_fk_infochannel = info_channel.infochannel_id " + 
          "INNER JOIN server ON info_channel.infochannel_fk_server = server.serv_id " + 
          "WHERE server.serv_guildid = %d " + 
          "AND current_game_info.currentgame_id = %d " + 
          "AND league_account.leagueaccount_id = %d";

  private static final String SELECT_GAME_INFO_CARD_WITH_CURRENT_GAME_ID_AND_GUILDID = 
      "SELECT " + 
          "game_info_card.gamecard_id, " + 
          "game_info_card.gamecard_fk_infochannel, " + 
          "game_info_card.gamecard_fk_currentgame, " + 
          "game_info_card.gamecard_titlemessageid, " + 
          "game_info_card.gamecard_infocardmessageid, " + 
          "game_info_card.gamecard_creationtime, " + 
          "game_info_card.gamecard_status " +
          "FROM game_info_card " + 
          "INNER JOIN current_game_info ON game_info_card.gamecard_fk_currentgame = current_game_info.currentgame_id " + 
          "INNER JOIN info_channel ON game_info_card.gamecard_fk_infochannel = info_channel.infochannel_id " + 
          "INNER JOIN server ON info_channel.infochannel_fk_server = server.serv_id " + 
          "WHERE server.serv_guildid = %d " + 
          "AND current_game_info.currentgame_id = %d";

  private static final String DELETE_GAME_INFO_CARDS_WITH_ID = "DELETE FROM game_info_card WHERE gamecard_id = %d";

  private static final String UPDATE_GAME_INFO_CARDS_CURRENT_GAME_WITH_ID = 
      "UPDATE game_info_card SET gamecard_fk_currentgame = %s WHERE gamecard_id = %d";

  private static final String UPDATE_GAME_INFO_CARDS_MESSAGES_WITH_ID =
      "UPDATE game_info_card SET gamecard_titlemessageid = %d, " +
          "gamecard_infocardmessageid = %d, " +
          "gamecard_creationtime = '%s' WHERE gamecard_id = %d";
  
  private static final String UPDATE_GAME_INFO_CARD_STATUS_WITH_ID =
      "UPDATE game_info_card SET gamecard_status = '%s' WHERE gamecard_id = %d";

  private static final String INSERT_INTO_GAME_INFO_CARD = "INSERT INTO game_info_card " +
      "(gamecard_fk_infochannel, gamecard_fk_currentgame, gamecard_status) VALUES (%d, %d, '%s')";

  private GameInfoCardRepository() {
    //hide default public constructor
  }
  
  public static void updateGameInfoCardStatusWithId(long gameCardId, GameInfoCardStatus status)
      throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_GAME_INFO_CARD_STATUS_WITH_ID, status.toString(), gameCardId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static DTO.GameInfoCard getGameInfoCardsWithCurrentGameId(long guildId, long currentGameId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_GAME_INFO_CARD_WITH_CURRENT_GAME_ID_AND_GUILDID,
          guildId, currentGameId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }

      return new DTO.GameInfoCard(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static void updateGameInfoCardsMessagesWithId(long titleId, long messageId, LocalDateTime creationTime, long gameCardId)
      throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_GAME_INFO_CARDS_MESSAGES_WITH_ID, titleId, messageId,
          DTO.DB_TIME_PATTERN.format(creationTime), gameCardId);
      query.executeUpdate(finalQuery);
    }
  }


  public static void createGameCards(long infochannelId, long currentGameId, GameInfoCardStatus status) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(INSERT_INTO_GAME_INFO_CARD, infochannelId, currentGameId, status.toString());
      query.execute(finalQuery);
    }
  }

  public static DTO.GameInfoCard getGameInfoCardsWithCurrentGame(long guildId, long currentGameInfoId, long leagueAccountId)
      throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_GAME_INFO_CARD_WITH_GUILD_ID_CURRENT_GAME_INFO_ID_ACCOUNT,
          guildId, currentGameInfoId, leagueAccountId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }

      return new DTO.GameInfoCard(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static List<DTO.GameInfoCard> getGameInfoCards(long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_GAME_INFO_CARD_WITH_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);

      List<DTO.GameInfoCard> gameCards = new ArrayList<>();
      if(0 != (result.last() ? result.getRow() : 0)) {
        result.first();
        while(!result.isAfterLast()) {
          gameCards.add(new DTO.GameInfoCard(result));
          result.next();
        }
      }

      return gameCards;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  public static void deleteGameInfoCardsWithId(long gameCardId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      List<DTO.LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithGameCardsId(gameCardId);

      for(DTO.LeagueAccount leagueAccount : leaguesAccounts) {
        LeagueAccountRepository.updateAccountGameCardWithAccountId(leagueAccount.leagueAccount_id, gameCardId);
      }

      String finalQuery = String.format(DELETE_GAME_INFO_CARDS_WITH_ID, gameCardId);
      query.executeUpdate(finalQuery);
    }
  }

  public static void updateGameInfoCardsCurrentGamesWithId(long currentGameInfoId, long gameCardId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery;
      
      if(currentGameInfoId == 0) {
        finalQuery = String.format(UPDATE_GAME_INFO_CARDS_CURRENT_GAME_WITH_ID, "NULL", gameCardId);
      }else {
        finalQuery = String.format(UPDATE_GAME_INFO_CARDS_CURRENT_GAME_WITH_ID, Long.toString(currentGameInfoId), gameCardId);
      }
      query.executeUpdate(finalQuery);
    }
  }

}
