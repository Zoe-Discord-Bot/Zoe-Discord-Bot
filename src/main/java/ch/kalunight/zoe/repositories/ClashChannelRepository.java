package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.kalunight.zoe.model.dto.ClashTeamMessageManager;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;

public class ClashChannelRepository {

  private static final String SELECT_ALL_CLASH_CHANNEL_WITH_GUILD_ID = "SELECT " + 
      "clash_channel.clashchannel_id, " + 
      "clash_channel.clashchannel_fk_server, " + 
      "clash_channel.clashchannel_channelid, " + 
      "clash_channel.clashchannel_teammessages, " + 
      "clash_channel.clashchannel_timezone " + 
      "FROM server " + 
      "INNER JOIN clash_channel ON server.serv_id = clash_channel.clashchannel_fk_server " + 
      "WHERE server.serv_guildid = %d";
  
  private static final String SELECT_ALL_CLASH_CHANNELS_WITHOUT_GIVEN_CLASH_CHANNEL_ID = "SELECT " + 
      "clash_channel.clashchannel_timezone, " + 
      "clash_channel.clashchannel_teammessages, " + 
      "clash_channel.clashchannel_channelid, " + 
      "clash_channel.clashchannel_fk_server, " + 
      "clash_channel.clashchannel_id " + 
      "FROM clash_channel " + 
      "INNER JOIN clash_channel ON server.serv_id = clash_channel.clashchannel_fk_server " +
      "WHERE clash_channel.clashchannel_id <> %d" +
      "AND server.serv_guildid = %d";
  
  private static final String UPDATE_CLASH_CHANNEL_TEAM_MESSAGES_WITH_ID = "UPDATE clash_channel SET clashchannel_teammessages = '%s' WHERE clashchannel_id = %d";
  
  private static final Gson gson = new GsonBuilder().create();
  
  private ClashChannelRepository() {
    //hide default public constructor
  }
  
  public static void updateChampionsRoles(ClashTeamMessageManager clashMessages, Long clashChannelId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(UPDATE_CLASH_CHANNEL_TEAM_MESSAGES_WITH_ID,
          gson.toJson(clashMessages), clashChannelId);
      
      query.execute(finalQuery);
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
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

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
