package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import ch.kalunight.zoe.model.dto.DTO;

public class TeamRepository {

  private static final String INSERT_INTO_TEAM = "INSERT INTO team (team_fk_server, team_name) "
      + "VALUES (%d, '%s')";
  
  private static final String SELECT_TEAM_BY_NAME = "SELECT " + 
      "team.team_id,team.team_fk_server,team.team_name " + 
      "FROM server " + 
      "INNER JOIN team ON server.serv_id = team.team_fk_server " + 
      "WHERE team.team_name = '%s' " + 
      "AND server.serv_guildid = %d";
  
  private static final String SELECT_TEAM_BY_SERVER_AND_DISCORD_ID = "SELECT " + 
      "team.team_id,team.team_fk_server,team.team_name " + 
      "FROM server " + 
      "INNER JOIN team ON server.serv_id = team.team_fk_server " + 
      "INNER JOIN player ON team.team_id = player.player_fk_team " + 
      "INNER JOIN player AS player_1 ON server.serv_id = player_1.player_fk_server " + 
      "WHERE player.player_discordid = %d " + 
      "AND server.serv_guildid = %d";
  
  private static final String DELETE_TEAM_WITH_TEAMID = "DELETE FROM team WHERE team_id = %d";
  
  private TeamRepository() {
    //hide default public constructor
  }
  
  public static void createTeam(long servId, String teamName) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(INSERT_INTO_TEAM, servId, teamName);
      query.execute(finalQuery);
    }
  }
  
  public static void deleteTeam(long teamId, List<Long> playersId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      for(Long playerId : playersId) {
        PlayerRepository.updateTeamOfPlayerDefineNull(playerId);
      }
      String finalQuery = String.format(DELETE_TEAM_WITH_TEAMID, teamId);
      query.execute(finalQuery);
    }
  }
  
  public static DTO.Team getTeamByPlayerAndGuild(long guildId, long discordId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_TEAM_BY_SERVER_AND_DISCORD_ID, discordId, guildId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.Team(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static DTO.Team getTeam(long guildId, String teamName) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_TEAM_BY_NAME, teamName, guildId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.Team(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
}
