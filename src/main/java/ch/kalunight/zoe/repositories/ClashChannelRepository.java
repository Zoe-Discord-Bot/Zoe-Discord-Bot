package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.kalunight.zoe.model.dto.ClashChannelData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;

public class ClashChannelRepository {

  private static final String SELECT_CLASH_CHANNEL_WITH_LAST_REFRESH = "SELECT " + 
      "clash_channel.clashchannel_timezone, " +
      "clash_channel.clashchannel_lastRefresh, " +
      "clash_channel.clashchannel_data, " + 
      "clash_channel.clashchannel_channelid, " + 
      "clash_channel.clashchannel_fk_server, " + 
      "clash_channel.clashchannel_id " + 
      "FROM clash_channel " + 
      "WHERE clash_channel.clashchannel_lastRefresh < '%s'";
  
  private static final String SELECT_CLASH_CHANNEL_WITH_ID = "SELECT " + 
      "clash_channel.clashchannel_timezone, " +
      "clash_channel.clashchannel_lastRefresh, " +
      "clash_channel.clashchannel_data, " + 
      "clash_channel.clashchannel_channelid, " + 
      "clash_channel.clashchannel_fk_server, " + 
      "clash_channel.clashchannel_id " + 
      "FROM clash_channel " + 
      "WHERE clash_channel.clashchannel_id = %s";
  
  private static final String SELECT_ALL_CLASH_CHANNEL_WITH_GUILD_ID = "SELECT " + 
      "clash_channel.clashchannel_id, " + 
      "clash_channel.clashchannel_fk_server, " + 
      "clash_channel.clashchannel_channelid, " + 
      "clash_channel.clashchannel_data, " + 
      "clash_channel.clashchannel_timezone, " + 
      "clash_channel.clashchannel_lastRefresh " + 
      "FROM server " + 
      "INNER JOIN clash_channel ON server.serv_id = clash_channel.clashchannel_fk_server " + 
      "WHERE server.serv_guildid = %d";
  
  private static final String SELECT_ALL_CLASH_CHANNELS_WITHOUT_GIVEN_CLASH_CHANNEL_ID = "SELECT " + 
      "clash_channel.clashchannel_timezone, " +
      "clash_channel.clashchannel_lastRefresh, " +
      "clash_channel.clashchannel_data, " + 
      "clash_channel.clashchannel_channelid, " + 
      "clash_channel.clashchannel_fk_server, " + 
      "clash_channel.clashchannel_id " + 
      "FROM clash_channel " + 
      "INNER JOIN clash_channel ON server.serv_id = clash_channel.clashchannel_fk_server " +
      "WHERE clash_channel.clashchannel_id <> %d" +
      "AND server.serv_guildid = %d";
  
  private static final String UPDATE_CLASH_CHANNEL_TEAM_MESSAGES_WITH_ID = "UPDATE clash_channel SET clashchannel_data = '%s' WHERE clashchannel_id = %d";
  
  private static final String UPDATE_CLASH_CHANNEL_REFRESH_WITH_ID = "UPDATE clash_channel SET clashchannel_lastRefresh = '%s' WHERE clashchannel_id = %d";
  
  private static final String DELETE_CLASH_CHANNEL_WITH_ID = 
      "DELETE FROM clash_channel WHERE clashchannel_id = %d";
  
  private static final String INSERT_CLASH_CHANNEL = "INSERT INTO clash_channel (clashchannel_fk_server, clashchannel_channelid, clashchannel_data, "
      + "clashchannel_timezone, clashchannel_lastRefresh) VALUES (%d, %d, '%s', '%s', '%s') RETURNING clashchannel_id";
  
  private static final Gson gson = new GsonBuilder().create();
  
  private ClashChannelRepository() {
    //hide default public constructor
  }
  
  public static void updateClashChannel(ClashChannelData clashMessages, Long clashChannelId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(UPDATE_CLASH_CHANNEL_TEAM_MESSAGES_WITH_ID,
          gson.toJson(clashMessages), clashChannelId);
      
      query.execute(finalQuery);
    }
  }
  
  public static void updateClashChannelRefresh(LocalDateTime newLastRefresh, Long clashChannelId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(UPDATE_CLASH_CHANNEL_REFRESH_WITH_ID,
          DTO.DB_TIME_PATTERN.format(newLastRefresh), clashChannelId);
      
      query.execute(finalQuery);
    }
  }
  
  public static void deleteClashChannel(long clashChannelId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      deleteClashChannel(clashChannelId, conn);
    }
  }
  
  public static void deleteClashChannel(long clashChannelId, Connection conn) throws SQLException {
    try (Statement query = conn.createStatement();) {

      String finalQuery = String.format(DELETE_CLASH_CHANNEL_WITH_ID, clashChannelId);
      query.execute(finalQuery);
    }
  }
  
  public static long createClashChannel(Server server, long channelId, String timezone, ClashChannelData clashChannelData) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(INSERT_CLASH_CHANNEL, server.serv_id, channelId, gson.toJson(clashChannelData),
          timezone, DTO.DB_TIME_PATTERN.format(LocalDateTime.now()));
      
      result = query.executeQuery(finalQuery);
      result.next();

      return result.getLong("clashchannel_id");
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static DTO.ClashChannel getClashChannelWithId(long clashChannelId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_CLASH_CHANNEL_WITH_ID, Long.toString(clashChannelId));
      result = query.executeQuery(finalQuery);
      
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.ClashChannel(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.ClashChannel> getClashChannelWhoNeedToBeRefreshed() throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_CLASH_CHANNEL_WITH_LAST_REFRESH,
          DTO.DB_TIME_PATTERN.format(LocalDateTime.now().minusHours(1)));
      result = query.executeQuery(finalQuery);
      
      List<DTO.ClashChannel> clashChannel = new ArrayList<>();
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return clashChannel;
      }
      
      result.first();
      while(!result.isAfterLast()) {
        clashChannel.add(new DTO.ClashChannel(result));
        result.next();
      }
      
      return clashChannel;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.ClashChannel> getClashChannelsWithoutGivenClashChannel(ClashChannel clashChannel, long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_ALL_CLASH_CHANNELS_WITHOUT_GIVEN_CLASH_CHANNEL_ID, clashChannel.clashChannel_id, guildId);
      result = query.executeQuery(finalQuery);

      List<DTO.ClashChannel> clashChannels = new ArrayList<>();
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return clashChannels;
      }
      result.first();
      while(!result.isAfterLast()) {
        clashChannels.add(new DTO.ClashChannel(result));
        result.next();
      }

      return clashChannels;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static List<DTO.ClashChannel> getClashChannels(long guildId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      return getClashChannels(guildId, conn);
    }
  }
  
  public static List<DTO.ClashChannel> getClashChannels(long guildId, Connection conn) throws SQLException {
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_ALL_CLASH_CHANNEL_WITH_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);

      List<DTO.ClashChannel> clashChannels = new ArrayList<>();
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return clashChannels;
      }
      result.first();
      while(!result.isAfterLast()) {
        clashChannels.add(new DTO.ClashChannel(result));
        result.next();
      }

      return clashChannels;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
}
