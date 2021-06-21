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
import ch.kalunight.zoe.model.player_data.Team;
import ch.kalunight.zoe.translation.LanguageManager;

public class TeamRepository {

  private static final String INSERT_INTO_TEAM = "INSERT INTO team (team_fk_server, team_name) "
      + "VALUES (?, ?)";
  
  private static final String SELECT_TEAM_BY_NAME = "SELECT " + 
      "team.team_id,team.team_fk_server,team.team_name " + 
      "FROM server " + 
      "INNER JOIN team ON server.serv_id = team.team_fk_server " + 
      "WHERE team.team_name = ? " + 
      "AND server.serv_guildid = ?";
  
  private static final String SELECT_TEAM_BY_SERVER_AND_DISCORD_ID = "SELECT " + 
      "team.team_id,team.team_fk_server,team.team_name " + 
      "FROM server " + 
      "INNER JOIN team ON server.serv_id = team.team_fk_server " + 
      "INNER JOIN player ON team.team_id = player.player_fk_team " + 
      "INNER JOIN player AS player_1 ON server.serv_id = player_1.player_fk_server " + 
      "WHERE player.player_discordid = %d " + 
      "AND server.serv_guildid = %d";
  
  private static final String SELECT_TEAMS_BY_GUILD_ID = 
      "SELECT " + 
      "team.team_id,team.team_fk_server,team.team_name " + 
      "FROM team " + 
      "INNER JOIN server ON team.team_fk_server = server.serv_id " + 
      "WHERE server.serv_guildid = %d";
  
  private static final String DELETE_TEAM_WITH_TEAMID = "DELETE FROM team WHERE team_id = %d";
  
  private TeamRepository() {
    //hide default public constructor
  }
  
  public static void createTeam(long servId, String teamName) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        PreparedStatement stmt = conn.prepareStatement(INSERT_INTO_TEAM, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      stmt.setLong(1, servId);
      stmt.setString(2, teamName);
      stmt.execute();
    }
  }
  
  public static void deleteTeam(long teamId, List<Long> playersId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      deleteTeam(teamId, playersId, conn);
    }
  }
  
  public static void deleteTeam(long teamId, List<Long> playersId, Connection conn) throws SQLException {
    try (Statement query = conn.createStatement();) {
      
      for(Long playerId : playersId) {
        PlayerRepository.updateTeamOfPlayerDefineNull(playerId, conn);
      }
      String finalQuery = String.format(DELETE_TEAM_WITH_TEAMID, teamId);
      query.execute(finalQuery);
    }
  }
  
  public static List<Team> getAllPlayerInTeams(long guildId, String language) throws SQLException {
    List<DTO.Player> playerWithNoTeam = new ArrayList<>();
    List<DTO.Player> players = PlayerRepository.getPlayers(guildId);
    List<DTO.Team> teamsDto = TeamRepository.getTeamsByGuild(guildId);
    List<Team> teams = new ArrayList<>();
    for(DTO.Player player : players) {
      player.leagueAccounts = LeagueAccountRepository.getLeaguesAccounts(guildId, player.player_discordId);
    }
    
    List<DTO.Player> playerWithATeam = new ArrayList<>();
    
    for(DTO.Team team : teamsDto) {
      Team teamToAdd = new Team(team.team_name);
      teams.add(teamToAdd);
      for(DTO.Player player : players) {
        if(team.team_id == player.player_fk_team) {
          teamToAdd.getPlayers().add(player);
          playerWithATeam.add(player);
        }
      }
    }
    
    for(DTO.Player player : playerWithATeam) {
      players.remove(player);
    }
    
    playerWithNoTeam.addAll(players);

    List<Team> allTeams = new ArrayList<>();
    allTeams.addAll(teams);
    if(!playerWithNoTeam.isEmpty()) {
      allTeams.add(new Team(LanguageManager.getText(language, "teamNameOfPlayerWithoutTeam"), playerWithNoTeam));
    }

    return allTeams;
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
  
  public static List<DTO.Team> getTeamsByGuild(long guildId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      return getTeamsByGuild(guildId, conn);
    }
  }
  
  public static List<DTO.Team> getTeamsByGuild(long guildId, Connection conn) throws SQLException {
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_TEAMS_BY_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);
      
      List<DTO.Team> teams = Collections.synchronizedList(new ArrayList<>());
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return teams;
      }
      result.first();
      while(!result.isAfterLast()) {
        teams.add(new DTO.Team(result));
        result.next();
      }
      
      return teams;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static DTO.Team getTeam(long guildId, String teamName) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        PreparedStatement stmt = conn.prepareStatement(SELECT_TEAM_BY_NAME, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      stmt.setString(1, teamName);
      stmt.setLong(2, guildId);
      result = stmt.executeQuery();
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
