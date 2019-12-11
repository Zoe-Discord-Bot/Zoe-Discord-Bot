package ch.kalunight.zoe.model.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import ch.kalunight.zoe.Zoe;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.constant.Platform;

public class DTO {
  
  public static final DateTimeFormatter DB_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  
  private DTO() {
    //hide default constructor
  }
  
  public static class Server {
    public long serv_id;
    public long serv_guildId;
    public String serv_language;
    public LocalDateTime serv_lastRefresh;
    
    public Server(ResultSet baseData) throws SQLException {
      serv_id = baseData.getLong("serv_id");
      serv_guildId = baseData.getLong("serv_guildId");
      serv_language = baseData.getString("serv_language");
      serv_lastRefresh = LocalDateTime.parse(baseData.getString("serv_lastRefresh"), DB_TIME_PATTERN);
    }
  }
  
  public static class ServerStatus {
    public long servstatus_id;
    public long servstatus_fk_server;
    public boolean servstatus_inTreatment;
    
    public ServerStatus(ResultSet baseData) throws SQLException {
      servstatus_id = baseData.getLong("servstatus_id");
      servstatus_fk_server = baseData.getLong("servstatus_fk_server");
      servstatus_inTreatment = baseData.getBoolean("servstatus_intreatment");
    }
  }
  
  public static class InfoChannel {
    public long infoChannel_id;
    public long infochannel_fk_server;
    public long infochannel_channelid;
    
    public InfoChannel(ResultSet baseData) throws SQLException {
      infoChannel_id = baseData.getLong("infochannel_id");
      infochannel_fk_server = baseData.getLong("infochannel_fk_server");
      infochannel_channelid = baseData.getLong("infochannel_channelid");
    }
  }
  
  public static class InfoPanelMessage {
    public long infopanel_id;
    public long infopanel_fk_infochannel;
    public long infopanel_messageId;
    
    public InfoPanelMessage(ResultSet baseData) throws SQLException {
      infopanel_id = baseData.getLong("infopanel_id");
      infopanel_fk_infochannel = baseData.getLong("infopanel_fk_infochannel");
      infopanel_messageId = baseData.getLong("infopanel_messageid");
    }
  }
  
  public static class Player {
    public long player_id;
    public long player_fk_server;
    public long player_fk_team;
    public long player_discordId;
    public boolean player_mentionnable;
    public User user;
    
    public Player(ResultSet baseData) throws SQLException {
      player_id = baseData.getLong("player_id");
      player_fk_server = baseData.getLong("player_fk_server");
      player_fk_team = baseData.getLong("player_fk_team");
      player_discordId = baseData.getLong("player_discordId");
      player_mentionnable = baseData.getBoolean("player_mentionnable");
      user = Zoe.getJda().retrieveUserById(player_discordId).complete();
    }
  }
  
  public static class LeagueAccount {
    public long leagueAccount_id;
    public long leagueAccount_fk_player;
    public long leagueAccount_fk_gamecard;
    public String leagueAccount_summonerId;
    public String leagueAccount_accoundId;
    public String leagueAccount_puuid;
    public Platform leagueAccount_server;
    public String leagueAccount_currentGame;
    
    public LeagueAccount(ResultSet baseData) throws SQLException {
      leagueAccount_id = baseData.getLong("leagueAccount_id");
      leagueAccount_fk_player = baseData.getLong("leagueAccount_fk_player");
      leagueAccount_fk_gamecard = baseData.getLong("leagueAccount_fk_gamecard");
      leagueAccount_summonerId = baseData.getString("leagueAccount_summonerId");
      leagueAccount_accoundId = baseData.getString("leagueAccount_accountId");
      leagueAccount_puuid = baseData.getString("leagueAccount_puuid");
      leagueAccount_server = Platform.getPlatformByName(baseData.getString("leagueAccount_server"));
      leagueAccount_currentGame = baseData.getString("leagueAccount_currentGame");
    }
  }
  
  public static class Team {
    public long team_id;
    public long team_fk_server;
    public String team_name;
    
    public Team(ResultSet baseData) throws SQLException {
      team_id = baseData.getLong("team_id");
      team_fk_server = baseData.getLong("team_fk_server");
      team_name = baseData.getString("team_name");
    }
  }
}
