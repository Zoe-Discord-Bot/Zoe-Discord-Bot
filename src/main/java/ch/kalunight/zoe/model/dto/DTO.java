package ch.kalunight.zoe.model.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.service.analysis.ChampionRole;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.endpoints.tft_league.dto.TFTLeagueEntry;
import net.rithms.riot.constant.Platform;

public class DTO {

  public static final DateTimeFormatter DB_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final Gson gson = new GsonBuilder().create();

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
  
  public static class ServerConfig {
    public long servconfig_id;
    public long servconfig_fk_server;

    public ServerConfig(ResultSet baseData) throws SQLException {
      servconfig_id = baseData.getLong("servconfig_id");
      servconfig_fk_server = baseData.getLong("servconfig_fk_server");
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

  public static class GameInfoCard {
    public long gamecard_id;
    public long gamecard_fk_infochannel;
    public long gamecard_fk_currentgame;
    /**
     * @deprecated
     * Deprecated, we now send all in one message using {@link #gamecard_infocardmessageid}. 
     * We save this line in the case we need to rollback to the old system.
     */
    @Deprecated public long gamecard_titlemessageid;
    public long gamecard_infocardmessageid;
    public GameInfoCardStatus gamecard_status;
    public LocalDateTime gamecard_creationtime;

    public GameInfoCard(ResultSet baseData) throws SQLException {
      gamecard_id = baseData.getLong("gamecard_id");
      gamecard_fk_infochannel = baseData.getLong("gamecard_fk_infochannel");
      gamecard_fk_currentgame = baseData.getLong("gamecard_fk_currentgame");
      gamecard_titlemessageid = baseData.getLong("gamecard_titlemessageid");
      gamecard_infocardmessageid = baseData.getLong("gamecard_infocardmessageid");
      gamecard_status = GameInfoCardStatus.valueOf(baseData.getString("gamecard_status"));
      if(baseData.getString("gamecard_creationtime") != null) {
        gamecard_creationtime = LocalDateTime.parse(baseData.getString("gamecard_creationtime"), DB_TIME_PATTERN);
      }
    }
    
    @Override
    public String toString() {
      return "[GameInfoCard: gamecard_id : " + gamecard_id + "]";
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
  
  public static class ClashChannel {
    public long clashChannel_id;
    public long clashChannel_fk_server;
    public long clashChannel_channelId;
    public ClashTeamData clashChannel_data;
    public TimeZone clashChannel_timezone;
    
    public ClashChannel(ResultSet baseData) throws SQLException {
      clashChannel_id = baseData.getLong("clashChannel_id");
      clashChannel_fk_server = baseData.getLong("clashChannel_fk_server");
      clashChannel_channelId = baseData.getLong("clashChannel_channelId");
      clashChannel_data = gson.fromJson(baseData.getString("clashChannel_data"), ClashTeamData.class);
      clashChannel_timezone = TimeZone.getTimeZone(baseData.getString("clashChannel_timezone"));
    }
  }

  public static class Player {
    public long player_id;
    public long player_fk_server;
    public long player_fk_team;
    public long player_discordId;
    public boolean player_mentionnable;
    public List<DTO.LeagueAccount> leagueAccounts = Collections.synchronizedList(new ArrayList<>());

    public Player(ResultSet baseData) throws SQLException {
      player_id = baseData.getLong("player_id");
      player_fk_server = baseData.getLong("player_fk_server");
      player_fk_team = baseData.getLong("player_fk_team");
      player_discordId = baseData.getLong("player_discordId");
      player_mentionnable = baseData.getBoolean("player_mentionnable");
    }
    
    public User getUser() {
      if(Zoe.getJda() != null) {
        return Zoe.getJda().retrieveUserById(player_discordId).complete();
      }
      return null;
    }
  }

  public static class LeagueAccount {
    public long leagueAccount_id;
    public long leagueAccount_fk_player;
    public long leagueAccount_fk_gamecard;
    public long leagueAccount_fk_currentgame;
    /**
     * @deprecated we now get this data inside {@link SummonerCache}
     */
    public String leagueAccount_name;
    public String leagueAccount_summonerId;
    public String leagueAccount_accoundId;
    public String leagueAccount_puuid;
    public String leagueAccount_tftSummonerId;
    public String leagueAccount_tftAccountId;
    public String leagueAccount_tftPuuid;
    public Platform leagueAccount_server;
    public SavedSummoner summoner;

    public LeagueAccount(ResultSet baseData) throws SQLException {
      leagueAccount_id = baseData.getLong("leagueAccount_id");
      leagueAccount_fk_player = baseData.getLong("leagueAccount_fk_player");
      leagueAccount_fk_gamecard = baseData.getLong("leagueAccount_fk_gamecard");
      leagueAccount_fk_currentgame = baseData.getLong("leagueAccount_fk_currentgame");
      leagueAccount_name = baseData.getString("leagueAccount_name");
      leagueAccount_summonerId = baseData.getString("leagueAccount_summonerId");
      leagueAccount_accoundId = baseData.getString("leagueAccount_accountId");
      leagueAccount_puuid = baseData.getString("leagueAccount_puuid");
      leagueAccount_tftSummonerId = baseData.getString("leagueAccount_tftSummonerId");
      leagueAccount_tftAccountId = baseData.getString("leagueAccount_tftAccountId");
      leagueAccount_tftPuuid = baseData.getString("leagueAccount_tftPuuid");
      leagueAccount_server = Platform.getPlatformByName(baseData.getString("leagueAccount_server"));
    }
    
    public LeagueAccount(Summoner summoner, Platform platform) {
      leagueAccount_name = summoner.getName();
      leagueAccount_summonerId = summoner.getId();
      leagueAccount_accoundId = summoner.getAccountId();
      leagueAccount_puuid = summoner.getPuuid();
      leagueAccount_server = platform;
    }
    
    @Override
    public String toString() {
      return "[LeagueAccount: leagueAccount_id : " + leagueAccount_id + "]";
    }
  }

  public static class CurrentGameInfo {
    public long currentgame_id;
    public net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo currentgame_currentgame;
    public Platform currentgame_server;
    public Long currentgame_gameid;

    public CurrentGameInfo(ResultSet baseData) throws SQLException {
      currentgame_id = baseData.getLong("currentgame_id");
      if(baseData.getString("currentgame_currentgame") != null) {
        currentgame_currentgame = gson.fromJson(baseData.getString("currentgame_currentgame"),
            net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo.class);
      }
      
      if(baseData.getString("currentgame_server") != null) {
        currentgame_server = Platform.getPlatformByName(baseData.getString("currentgame_server"));
      }
      
      if(baseData.getString("currentgame_gameid") != null) {
        currentgame_gameid = baseData.getLong("currentgame_gameid");
      }
    }
    
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((currentgame_gameid == null) ? 0 : currentgame_gameid.hashCode());
      result = prime * result + (int) (currentgame_id ^ (currentgame_id >>> 32));
      result = prime * result + ((currentgame_server == null) ? 0 : currentgame_server.hashCode());
      return result;
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      
      if (obj == null) {
        return false;
      }
      
      if (getClass() != obj.getClass()) {
        return false;
      }
      
      CurrentGameInfo other = (CurrentGameInfo) obj;
      if (currentgame_gameid == null) {
        if (other.currentgame_gameid != null) {
          return false;
        }
      } else if (!currentgame_gameid.equals(other.currentgame_gameid)) {
        return false;
      }
      
      if (currentgame_id != other.currentgame_id) {
        return false;
      }
      
      if (currentgame_server != other.currentgame_server) {
        return false;
      }
      
      return true;
    }


    @Override
    public String toString() {
      return "[CurrentGameInfo currentgame_id : " + currentgame_id + "]";
    }
  }
  
  public static class MatchCache {
    public long mCatch_id;
    public String mCatch_gameId;
    public Platform mCatch_platform;
    public SavedMatch mCatch_savedMatch;
    public LocalDateTime mCatch_creationTime;
    
    public MatchCache(ResultSet baseData) throws SQLException {
      mCatch_id = baseData.getLong("mCatch_id");
      mCatch_gameId = baseData.getString("mCatch_gameId");
      mCatch_platform = Platform.getPlatformByName(baseData.getString("mCatch_platform"));
      mCatch_savedMatch = gson.fromJson(baseData.getString("mCatch_savedMatch"), SavedMatch.class);
      mCatch_creationTime = LocalDateTime.parse(baseData.getString("mCatch_creationTime"), DB_TIME_PATTERN);
    }
  }
  
  public static class ChampionRoleAnalysis {
    public long cra_id;
    public int cra_keyChampion;
    public LocalDateTime cra_lastRefresh;
    public List<ChampionRole> cra_roles;
    public String cra_roles_stats;
    
    public ChampionRoleAnalysis(ResultSet baseData) throws SQLException {
      cra_id = baseData.getLong("cra_id");
      cra_keyChampion = baseData.getInt("cra_keyChampion");
      cra_lastRefresh = LocalDateTime.parse(baseData.getString("cra_lastRefresh"), DB_TIME_PATTERN);
      cra_roles = new ArrayList<>();
      
      String[] roles = baseData.getString("cra_roles").split(";");
      for(String strRole : roles) {
        if(strRole.isEmpty()) {
          continue;
        }
        ChampionRole role = ChampionRole.valueOf(strRole);
        
        if(role != null) {
          cra_roles.add(role);
        }
      }
      
      cra_roles_stats = baseData.getString("cra_roles_stats");
    }
  }
  
  public static class Leaderboard {
    public long lead_id;
    public long lead_fk_server;
    public long lead_message_channelId;
    public long lead_message_id;
    public int lead_type;
    public String lead_data;
    public LocalDateTime lead_lastrefresh;
    
    public Leaderboard(ResultSet baseData) throws SQLException {
      lead_id = baseData.getLong("lead_id");
      lead_fk_server = baseData.getLong("lead_fk_server");
      lead_message_channelId = baseData.getLong("lead_message_channelId");
      lead_message_id = baseData.getLong("lead_message_id");
      lead_type = baseData.getInt("lead_type");
      lead_data = baseData.getString("lead_data");
      lead_lastrefresh = LocalDateTime.parse(baseData.getString("lead_lastrefresh"), DB_TIME_PATTERN);
    }
  }

  public static class Team {
    public long team_id;
    public long team_fk_server;
    public String team_name;
    public List<DTO.Player> players = Collections.synchronizedList(new ArrayList<>());

    public Team(ResultSet baseData) throws SQLException {
      team_id = baseData.getLong("team_id");
      team_fk_server = baseData.getLong("team_fk_server");
      team_name = baseData.getString("team_name");
    }
    
    public Team(long id, long serverId, String teamName) {
      team_id = id;
      team_fk_server = serverId;
      team_name = teamName;
    }
  }
  
  public static class RankHistoryChannel {
    public long rhChannel_id;
    public long rhChannel_fk_server;
    public long rhChannel_channelId;
    
    public RankHistoryChannel(ResultSet baseData) throws SQLException {
      rhChannel_id = baseData.getLong("rhChannel_id");
      rhChannel_fk_server = baseData.getLong("rhChannel_fk_server");
      rhChannel_channelId = baseData.getLong("rhChannel_channelId");
    }
  }
  
  public static class BannedAccount {
    public long banAcc_id;
    public String banAcc_summonerId;
    public Platform banAcc_server;
    
    public BannedAccount(ResultSet baseData) throws SQLException {
      banAcc_id = baseData.getLong("banAcc_id");
      banAcc_summonerId = baseData.getString("banAcc_summonerId");
      banAcc_server = Platform.getPlatformByName(baseData.getString("banAcc_server"));
    }
    
  }
  
  public static class SummonerCache {
    public long sumCache_id;
    public String sumCache_summonerId;
    public String sumCache_tftSummonerId;
    public Platform sumCache_server;
    public SavedSummoner sumCache_data;
    
    public SummonerCache(ResultSet baseData) throws SQLException {
      sumCache_id = baseData.getLong("sumCache_id");
      sumCache_summonerId = baseData.getString("sumCache_summonerId");
      sumCache_tftSummonerId = baseData.getString("sumCache_tftSummonerId");
      sumCache_server = Platform.getPlatformByName(baseData.getString("sumCache_server"));
      sumCache_data = gson.fromJson(baseData.getString("sumCache_data"), SavedSummoner.class);
    }
  }
  
  public static class ChampionMasteryCache {
    public long champMasCache_id;
    public String champMasCache_summonerId;
    public Platform champMasCache_server;
    public SavedChampionMastery champMasCache_data;
    
    public ChampionMasteryCache(ResultSet baseData) throws SQLException {
      champMasCache_id = baseData.getLong("champMasCache_id");
      champMasCache_summonerId = baseData.getString("champMasCache_summonerId");
      champMasCache_server = Platform.getPlatformByName(baseData.getString("champMasCache_server"));
      champMasCache_data = gson.fromJson(baseData.getString("champMasCache_data"), SavedChampionMastery.class);
    }
  }
  
  public static class LastRank {
    public long lastRank_id;
    public long lastRank_fk_leagueAccount;
    public LeagueEntry lastRank_soloq;
    public LeagueEntry lastRank_soloqSecond;
    public LocalDateTime lastRank_soloqLastRefresh;
    public LeagueEntry lastRank_flex;
    public LeagueEntry lastRank_flexSecond;
    public LocalDateTime lastRank_flexLastRefresh;
    public TFTLeagueEntry lastRank_tft;
    public TFTLeagueEntry lastRank_tftSecond;
    public LocalDateTime lastRank_tftLastRefresh;
    public String lastRank_tftLastTreatedMatchId;
    
    public LastRank(ResultSet baseData) throws SQLException {
      lastRank_id = baseData.getLong("lastRank_id");
      lastRank_fk_leagueAccount = baseData.getLong("lastRank_fk_leagueAccount");
      String lastRank = baseData.getString("lastRank_soloq");
      if(lastRank != null) {
        lastRank_soloq = gson.fromJson(lastRank, LeagueEntry.class);
      }
      
      lastRank = baseData.getString("lastRank_soloqSecond");
      if(lastRank != null) {
        lastRank_soloqSecond = gson.fromJson(lastRank, LeagueEntry.class);
      }
      
      lastRank = baseData.getString("lastRank_soloqLastRefresh");
      if(lastRank != null) {
        lastRank_soloqLastRefresh = LocalDateTime.parse(lastRank.split("\\.")[0], DB_TIME_PATTERN);
      }
      
      lastRank = baseData.getString("lastRank_flex");
      if(lastRank != null) {
        lastRank_flex = gson.fromJson(lastRank, LeagueEntry.class);
      }
      
      lastRank = baseData.getString("lastRank_flexSecond");
      if(lastRank != null) {
        lastRank_flexSecond = gson.fromJson(lastRank, LeagueEntry.class);
      }
      
      lastRank = baseData.getString("lastRank_flexLastRefresh");
      if(lastRank != null) {
        lastRank_flexLastRefresh = LocalDateTime.parse(lastRank.split("\\.")[0], DB_TIME_PATTERN);
      }
      
      lastRank = baseData.getString("lastRank_tft");
      if(lastRank != null) {
        lastRank_tft = gson.fromJson(lastRank, TFTLeagueEntry.class);
      }
      
      lastRank = baseData.getString("lastRank_tftSecond");
      if(lastRank != null) {
        lastRank_tftSecond = gson.fromJson(lastRank, TFTLeagueEntry.class);
      }
      
      lastRank = baseData.getString("lastRank_tftLastRefresh");
      if(lastRank != null) {
        lastRank_tftLastRefresh = LocalDateTime.parse(lastRank.split("\\.")[0], DB_TIME_PATTERN);
      }
      
      lastRank = baseData.getString("lastRank_tftLastTreatedMatchId");
      if(lastRank != null) {
        lastRank_tftLastTreatedMatchId = lastRank;
      }
    }
  }
}
