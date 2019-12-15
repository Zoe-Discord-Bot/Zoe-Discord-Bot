package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.model.dto.DTO;

public class GameInfoCardRepository {

  private static final String SELECT_GAME_INFO_CARD_WITH_GUILD_ID =
      "SELECT " + 
      "game_info_card.gamecard_id, " + 
      "game_info_card.gamecard_fk_infochannel, " + 
      "game_info_card.gamecard_titlemessageid, " + 
      "game_info_card.gamecard_infocardmessageid, " + 
      "game_info_card.gamecard_creationtime " + 
      "FROM game_info_card " + 
      "INNER JOIN info_channel ON game_info_card.gamecard_fk_infochannel = info_channel.infochannel_id " + 
      "INNER JOIN server ON info_channel.infochannel_fk_server = server.serv_id " + 
      "WHERE server.serv_guildid = %d";
  
  private static final String DELETE_GAME_INFO_CARDS_WITH_ID = "DELETE FROM game_info_card WHERE gamecard_id = %d";
  
  private GameInfoCardRepository() {
    //hide default public constructor
  }
  
  public static List<DTO.GameInfoCard> getGameInfoCard(long guildId) throws SQLException {
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
      
      String finalQuery = String.format(DELETE_GAME_INFO_CARDS_WITH_ID, gameCardId);
      query.executeUpdate(finalQuery);
    }
  }
  
}
