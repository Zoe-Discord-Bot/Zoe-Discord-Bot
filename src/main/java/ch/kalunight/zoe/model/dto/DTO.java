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
import ch.kalunight.zoe.model.config.option.CleanChannelOption.CleanChannelOptionInfo;
import ch.kalunight.zoe.service.analysis.ChampionRole;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;

public class DTO {

  public static final DateTimeFormatter DB_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final Gson gson = new GsonBuilder().create();

  private DTO() {
    //hide default constructor
  }

  public static class Server {
    public final long serv_id;
    public final long serv_guildId;
    private String serv_language;
    public final LocalDateTime serv_lastRefresh;

    public Server(ResultSet baseData) throws SQLException {
      serv_id = baseData.getLong("serv_id");
      serv_guildId = baseData.getLong("serv_guildId");
      this.serv_language = baseData.getString("serv_language");
      serv_lastRefresh = LocalDateTime.parse(baseData.getString("serv_lastRefresh"), DB_TIME_PATTERN);
    }

    public String getLanguage() {
      return serv_language;
    }

    public void setLanguage(String serv_language) {
      this.serv_language = serv_language;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (serv_guildId ^ (serv_guildId >>> 32));
      result = prime * result + (int) (serv_id ^ (serv_id >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Server other = (Server) obj;
      if (serv_guildId != other.serv_guildId)
        return false;
      if (serv_id != other.serv_id)
        return false;
      return true;
    }
  }

  public static class ServerConfig {
    public final long servconfig_id;
    public final long servconfig_fk_server;

    public ServerConfig(ResultSet baseData) throws SQLException {
      servconfig_id = baseData.getLong("servconfig_id");
      servconfig_fk_server = baseData.getLong("servconfig_fk_server");
    }
  }

  public static class ServerStatus {
    public final long servstatus_id;
    public final long servstatus_fk_server;
    public final boolean servstatus_inTreatment;

    public ServerStatus(ResultSet baseData) throws SQLException {
      servstatus_id = baseData.getLong("servstatus_id");
      servstatus_fk_server = baseData.getLong("servstatus_fk_server");
      servstatus_inTreatment = baseData.getBoolean("servstatus_intreatment");
    }
  }

  public static class InfoChannel {
    public final long infoChannel_id;
    public final long infochannel_fk_server;
    public final long infochannel_channelid;

    public InfoChannel(ResultSet baseData) throws SQLException {
      infoChannel_id = baseData.getLong("infochannel_id");
      infochannel_fk_server = baseData.getLong("infochannel_fk_server");
      infochannel_channelid = baseData.getLong("infochannel_channelid");
    }
  }

  public static class GameInfoCard {
    public final long gamecard_id;
    public final long gamecard_fk_infochannel;
    public final long gamecard_fk_currentgame;
    /**
     * @deprecated
     * Deprecated, we now send all in one message using {@link #gamecard_infocardmessageid}. 
     * We save this line in the case we need to rollback to the old system.
     */
    @Deprecated public final long gamecard_titlemessageid;
    public final long gamecard_infocardmessageid;
    private GameInfoCardStatus gamecard_status;
    public final LocalDateTime gamecard_creationtime;

    public GameInfoCard(ResultSet baseData) throws SQLException {
      gamecard_id = baseData.getLong("gamecard_id");
      gamecard_fk_infochannel = baseData.getLong("gamecard_fk_infochannel");
      gamecard_fk_currentgame = baseData.getLong("gamecard_fk_currentgame");
      gamecard_titlemessageid = baseData.getLong("gamecard_titlemessageid");
      gamecard_infocardmessageid = baseData.getLong("gamecard_infocardmessageid");
      this.gamecard_status = GameInfoCardStatus.valueOf(baseData.getString("gamecard_status"));
      if(baseData.getString("gamecard_creationtime") != null) {
        gamecard_creationtime = LocalDateTime.parse(baseData.getString("gamecard_creationtime"), DB_TIME_PATTERN);
      }else {
        gamecard_creationtime = null;
      }
    }

    public GameInfoCardStatus getGamecardStatus() {
      return gamecard_status;
    }

    public void setGameCardStatus(GameInfoCardStatus gamecard_status) {
      this.gamecard_status = gamecard_status;
    }
  }

  public static class InfoPanelMessage {
    public final long infopanel_id;
    public final long infopanel_fk_infochannel;
    public final long infopanel_messageId;

    public InfoPanelMessage(ResultSet baseData) throws SQLException {
      infopanel_id = baseData.getLong("infopanel_id");
      infopanel_fk_infochannel = baseData.getLong("infopanel_fk_infochannel");
      infopanel_messageId = baseData.getLong("infopanel_messageid");
    }
  }

  public static class ClashChannel {
    public final long clashChannel_id;
    public final long clashChannel_fk_server;
    public final long clashChannel_channelId;
    public final ClashChannelData clashChannel_data;
    public final TimeZone clashChannel_timezone;
    public final LocalDateTime clashChannel_lastRefresh;

    public ClashChannel(ResultSet baseData) throws SQLException {
      clashChannel_id = baseData.getLong("clashChannel_id");
      clashChannel_fk_server = baseData.getLong("clashChannel_fk_server");
      clashChannel_channelId = baseData.getLong("clashChannel_channelId");
      clashChannel_data = gson.fromJson(baseData.getString("clashChannel_data"), ClashChannelData.class);
      clashChannel_timezone = TimeZone.getTimeZone(baseData.getString("clashChannel_timezone"));
      clashChannel_lastRefresh = LocalDateTime.parse(baseData.getString("clashChannel_lastRefresh"), DB_TIME_PATTERN);
    }
  }


  public static class Player {
    public final long player_id;
    public final long player_fk_server;
    public final long player_fk_team;
    public final long player_discordId;
    public final boolean player_mentionnable;
    public List<DTO.LeagueAccount> leagueAccounts = Collections.synchronizedList(new ArrayList<>());

    public Player(ResultSet baseData) throws SQLException {
      player_id = baseData.getLong("player_id");
      player_fk_server = baseData.getLong("player_fk_server");
      player_fk_team = baseData.getLong("player_fk_team");
      player_discordId = baseData.getLong("player_discordId");
      player_mentionnable = baseData.getBoolean("player_mentionnable");
    }

    public User retrieveUser(JDA jda) {
      User user = jda.retrieveUserById(player_discordId, false).complete();
      if(user != null) {
        return user;
      }

      return null;
    }

    public Member retrieveMember(Guild guild) {
      Member member = guild.retrieveMemberById(player_discordId, false).complete();
      if(member != null) {
        return member;
      }

      return null;
    }
  }

  public static class LeagueAccount {
    public final Long leagueAccount_id;
    public final Long leagueAccount_fk_player;
    public final Long leagueAccount_fk_gamecard;
    public final Long leagueAccount_fk_currentgame;
    /**
     * @deprecated we now get this data inside {@link SummonerCache}
     */
    public final String leagueAccount_name;
    public final String leagueAccount_summonerId;
    public final String leagueAccount_accoundId;
    public final String leagueAccount_puuid;
    public final String leagueAccount_tftSummonerId;
    public final String leagueAccount_tftAccountId;
    public final String leagueAccount_tftPuuid;
    public final ZoePlatform leagueAccount_server;

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
      leagueAccount_server = ZoePlatform.getZoePlatformByName(baseData.getString("leagueAccount_server"));
    }

    public LeagueAccount(SavedSummoner summoner, SavedSummoner tftSummoner, ZoePlatform platform) {
      leagueAccount_name = summoner.getName();
      leagueAccount_summonerId = summoner.getSummonerId();
      leagueAccount_accoundId = summoner.getAccountId();
      leagueAccount_puuid = summoner.getPuuid();
      leagueAccount_server = platform;
      leagueAccount_tftSummonerId = tftSummoner.getSummonerId();
      leagueAccount_tftAccountId = tftSummoner.getAccountId();
      leagueAccount_tftPuuid = tftSummoner.getPuuid();
      leagueAccount_id = null;
      leagueAccount_fk_player = null;
      leagueAccount_fk_gamecard = null;
      leagueAccount_fk_currentgame = null;
    }

    public SavedSummoner getSummoner(boolean forceRefresh) {
      return Zoe.getRiotApi().getSummonerByPUUIDWithAccountTransferManagement(leagueAccount_server, this, forceRefresh);
    }

    @Override
    public String toString() {
      return "[LeagueAccount: leagueAccount_id : " + leagueAccount_id + "]";
    }
  }

  public static class CurrentGameInfo {
    public final long currentgame_id;
    public SpectatorGameInfo currentgame_currentgame;
    public final ZoePlatform currentgame_server;
    public final Long currentgame_gameid;

    public CurrentGameInfo(ResultSet baseData) throws SQLException {
      currentgame_id = baseData.getLong("currentgame_id");
      if(baseData.getString("currentgame_currentgame") != null) {
        currentgame_currentgame = gson.fromJson(baseData.getString("currentgame_currentgame"), SpectatorGameInfo.class);
      }else {
        currentgame_currentgame = null;
      }

      if(baseData.getString("currentgame_server") != null) {
        currentgame_server = ZoePlatform.getZoePlatformByName(baseData.getString("currentgame_server"));
      }else {
        currentgame_server = null;
      }

      Long currentgame_gameid_temp = baseData.getLong("currentgame_gameid");
      if(currentgame_gameid_temp != 0) {
        currentgame_gameid = currentgame_gameid_temp;
      }else {
        currentgame_gameid = null;
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

  public static class ZoeUser {
    public final Long zoeUser_id;
    public final Long zoeUser_discordId;
    public final Long zoeUser_fullMonthSupported;
    public final Long zoeUser_totalGiven;

    public ZoeUser(ResultSet baseData) throws SQLException {
      zoeUser_id = baseData.getLong("zoeUser_id");
      zoeUser_discordId = baseData.getLong("zoeUser_discordId");
      zoeUser_fullMonthSupported = baseData.getLong("zoeUser_fullMonthSupported");
      zoeUser_totalGiven = baseData.getLong("zoeUser_totalGiven");
    }
  }

  public static class Role {
    public final Long role_id;
    public final Long role_roleId;

    public Role(ResultSet baseData) throws SQLException {
      role_id = baseData.getLong("role_id");
      role_roleId = baseData.getLong("role_roleId");
    }
  }

  public static class ZoeUserRole {
    public final Long zoeUserRole_fk_user_id;
    public final Long zoeUserRole_fk_role_id;
    public final LocalDateTime zoeUserRole_endOfTheSubscription;

    public ZoeUserRole(ResultSet baseData) throws SQLException {
      zoeUserRole_fk_user_id = baseData.getLong("zoeUserRole_fk_user_id");
      zoeUserRole_fk_role_id = baseData.getLong("zoeUserRole_fk_role_id");

      if(baseData.getString("zoeUserRole_endOfTheSubscription") != null) {
        zoeUserRole_endOfTheSubscription = LocalDateTime.parse(baseData.getString("zoeUserRole_endOfTheSubscription"), DB_TIME_PATTERN);
      }else {
        zoeUserRole_endOfTheSubscription = null;
      }
    }
  }
  
  public static class ServerRawSettings {
    public final Boolean selfoption_activate;
    public final Long roleoption_roleid;
    public final ZoePlatform regionoption_region;
    public final Boolean gamecardoption_activate;
    public final Long cleanoption_channelid;
    public final CleanChannelOptionInfo cleanoption_option;

    public ServerRawSettings(ResultSet baseData) throws SQLException {
      selfoption_activate = baseData.getBoolean("selfoption_activate");
      roleoption_roleid = baseData.getLong("roleoption_roleid");

      String platformName = baseData.getString("regionoption_region");
      if(platformName != null && !platformName.equals("")) {
        regionoption_region = ZoePlatform.getZoePlatformByName(platformName);
      }else {
        regionoption_region = null;
      }

      gamecardoption_activate = baseData.getBoolean("gamecardoption_activate");
      cleanoption_channelid = baseData.getLong("cleanoption_channelid");
      cleanoption_option = CleanChannelOptionInfo.valueOf(baseData.getString("cleanOption_option"));
    }
  }

  public static class RankRoleOption {
    public final long rankRoleOption_id;
    public final long rankRoleOption_fk_serverConfig;
    public final Long rankRoleOption_ironId;
    public final Long rankRoleOption_bronzeId;
    public final Long rankRoleOption_silverId;
    public final Long rankRoleOption_goldId;
    public final Long rankRoleOption_platinumId;
    public final Long rankRoleOption_diamondId;
    public final Long rankRoleOption_masterId;
    public final Long rankRoleOption_grandMasterId;
    public final Long rankRoleOption_challengerId;
    public final Boolean rankRoleOption_soloqEnable;
    public final Boolean rankRoleOption_flexEnable;
    public final Boolean rankRoleOption_tftEnable;

    public RankRoleOption(ResultSet baseData) throws SQLException {
      rankRoleOption_id = baseData.getLong("rankRoleOption_id");
      rankRoleOption_fk_serverConfig = baseData.getLong("rankRoleOption_fk_serverConfig");

      Long ironId = baseData.getLong("rankRoleOption_ironId");
      if(ironId != null && ironId != 0) {
        rankRoleOption_ironId = ironId;
      }else {
        rankRoleOption_ironId = null;
      }

      Long bronzeId = baseData.getLong("rankRoleOption_bronzeId");
      if(bronzeId != null && bronzeId != 0) {
        rankRoleOption_bronzeId = bronzeId;
      }else {
        rankRoleOption_bronzeId = null;
      }

      Long silverId = baseData.getLong("rankRoleOption_silverId");
      if(silverId != null && silverId != 0) {
        rankRoleOption_silverId = silverId;
      }else {
        rankRoleOption_silverId = null;
      }

      Long goldId = baseData.getLong("rankRoleOption_goldId");
      if(goldId != null && goldId != 0) {
        rankRoleOption_goldId = goldId;
      }else {
        rankRoleOption_goldId = null;
      }

      Long platinumId = baseData.getLong("rankRoleOption_platinumId");
      if(platinumId != null && platinumId != 0) {
        rankRoleOption_platinumId = platinumId;
      }else {
        rankRoleOption_platinumId = null;
      }

      Long diamondId = baseData.getLong("rankRoleOption_diamondId");
      if(diamondId != null && diamondId != 0) {
        rankRoleOption_diamondId = diamondId;
      }else {
        rankRoleOption_diamondId = null;
      }

      Long masterId = baseData.getLong("rankRoleOption_masterId");
      if(masterId != null && masterId != 0) {
        rankRoleOption_masterId = masterId;
      }else {
        rankRoleOption_masterId = null;
      }

      Long grandMasterId = baseData.getLong("rankRoleOption_grandMasterId");
      if(grandMasterId != null && grandMasterId != 0) {
        rankRoleOption_grandMasterId = grandMasterId;
      }else {
        rankRoleOption_grandMasterId = null;
      }

      Long challengerId = baseData.getLong("rankRoleOption_challengerId");
      if(challengerId != null && challengerId != 0) {
        rankRoleOption_challengerId = challengerId;
      }else {
        rankRoleOption_challengerId = null;
      }

      rankRoleOption_soloqEnable = baseData.getBoolean("rankRoleOption_soloqEnable");
      rankRoleOption_flexEnable = baseData.getBoolean("rankRoleOption_flexEnable");
      rankRoleOption_tftEnable = baseData.getBoolean("rankRoleOption_tftEnable");
    }
  }

  public static class ChampionRoleAnalysis {
    public final long cra_id;
    public final int cra_keyChampion;
    public final LocalDateTime cra_lastRefresh;
    public final List<ChampionRole> cra_roles;
    public final String cra_roles_stats;
    public final double cra_average_kda;

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
      cra_average_kda = baseData.getDouble("cra_average_kda");
    }
  }

  public static class Leaderboard {
    public final long lead_id;
    public final long lead_fk_server;
    public final long lead_message_channelId;
    public final long lead_message_id;
    public final int lead_type;
    public final String lead_data;
    public final LocalDateTime lead_lastrefresh;

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
    public final long team_id;
    public final long team_fk_server;
    public final String team_name;
    public final List<DTO.Player> players = Collections.synchronizedList(new ArrayList<>());

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
    public final long rhChannel_id;
    public final long rhChannel_fk_server;
    public final long rhChannel_channelId;

    public RankHistoryChannel(ResultSet baseData) throws SQLException {
      rhChannel_id = baseData.getLong("rhChannel_id");
      rhChannel_fk_server = baseData.getLong("rhChannel_fk_server");
      rhChannel_channelId = baseData.getLong("rhChannel_channelId");
    }
  }

  public static class BannedAccount {
    public final long banAcc_id;
    public final String banAcc_summonerId;
    public final ZoePlatform banAcc_server;

    public BannedAccount(ResultSet baseData) throws SQLException {
      banAcc_id = baseData.getLong("banAcc_id");
      banAcc_summonerId = baseData.getString("banAcc_summonerId");
      banAcc_server = ZoePlatform.getZoePlatformByName(baseData.getString("banAcc_server"));
    }

  }

  public static class LastRank {
    public final long lastRank_id;
    public final long lastRank_fk_leagueAccount;
    private LeagueEntry lastRank_soloq;
    private LeagueEntry lastRank_soloqSecond;
    public final LocalDateTime lastRank_soloqLastRefresh;
    private LeagueEntry lastRank_flex;
    private LeagueEntry lastRank_flexSecond;
    public final LocalDateTime lastRank_flexLastRefresh;
    private LeagueEntry lastRank_tft;
    private LeagueEntry lastRank_tftSecond;
    public final LocalDateTime lastRank_tftLastRefresh;
    public final String lastRank_tftLastTreatedMatchId;

    public LastRank(ResultSet baseData) throws SQLException {
      lastRank_id = baseData.getLong("lastRank_id");
      lastRank_fk_leagueAccount = baseData.getLong("lastRank_fk_leagueAccount");
      String lastRank = baseData.getString("lastRank_soloq");
      if(lastRank != null) {
        lastRank_soloq = gson.fromJson(lastRank, LeagueEntry.class);
      }else {
        lastRank_soloq = null;
      }

      lastRank = baseData.getString("lastRank_soloqSecond");
      if(lastRank != null) {
        lastRank_soloqSecond = gson.fromJson(lastRank, LeagueEntry.class);
      }else {
        lastRank_soloqSecond = null;
      }

      lastRank = baseData.getString("lastRank_soloqLastRefresh");
      if(lastRank != null) {
        lastRank_soloqLastRefresh = LocalDateTime.parse(lastRank.split("\\.")[0], DB_TIME_PATTERN);
      }else {
        lastRank_soloqLastRefresh = null;
      }

      lastRank = baseData.getString("lastRank_flex");
      if(lastRank != null) {
        lastRank_flex = gson.fromJson(lastRank, LeagueEntry.class);
      }else {
        lastRank_flex = null;
      }

      lastRank = baseData.getString("lastRank_flexSecond");
      if(lastRank != null) {
        lastRank_flexSecond = gson.fromJson(lastRank, LeagueEntry.class);
      }else {
        lastRank_flexSecond = null;
      }

      lastRank = baseData.getString("lastRank_flexLastRefresh");
      if(lastRank != null) {
        lastRank_flexLastRefresh = LocalDateTime.parse(lastRank.split("\\.")[0], DB_TIME_PATTERN);
      }else {
        lastRank_flexLastRefresh = null;
      }

      lastRank = baseData.getString("lastRank_tft");
      if(lastRank != null) {
        lastRank_tft = gson.fromJson(lastRank, LeagueEntry.class);
      }else {
        lastRank_tft = null;
      }

      lastRank = baseData.getString("lastRank_tftSecond");
      if(lastRank != null) {
        lastRank_tftSecond = gson.fromJson(lastRank, LeagueEntry.class);
      }else {
        lastRank_tftSecond = null;
      }

      lastRank = baseData.getString("lastRank_tftLastRefresh");
      if(lastRank != null) {
        lastRank_tftLastRefresh = LocalDateTime.parse(lastRank.split("\\.")[0], DB_TIME_PATTERN);
      }else {
        lastRank_tftLastRefresh = null;
      }

      lastRank = baseData.getString("lastRank_tftLastTreatedMatchId");
      if(lastRank != null) {
        lastRank_tftLastTreatedMatchId = lastRank;
      }else {
        lastRank_tftLastTreatedMatchId = null;
      }
    }

    public LeagueEntry getLastRankSoloq() {
      return lastRank_soloq;
    }

    public void setLastRankSoloq(LeagueEntry lastRank_soloq) {
      this.lastRank_soloq = lastRank_soloq;
    }

    public LeagueEntry getLastRankSoloqSecond() {
      return lastRank_soloqSecond;
    }

    public void setLastRankSoloqSecond(LeagueEntry lastRank_soloqSecond) {
      this.lastRank_soloqSecond = lastRank_soloqSecond;
    }

    public LeagueEntry getLastRankFlex() {
      return lastRank_flex;
    }

    public void setLastRankFlex(LeagueEntry lastRank_flex) {
      this.lastRank_flex = lastRank_flex;
    }

    public LeagueEntry getLastRankFlexSecond() {
      return lastRank_flexSecond;
    }

    public void setLastRankFlexSecond(LeagueEntry lastRank_flexSecond) {
      this.lastRank_flexSecond = lastRank_flexSecond;
    }

    public LeagueEntry getLastRankTft() {
      return lastRank_tft;
    }

    public void setLastRankTft(LeagueEntry lastRank_tft) {
      this.lastRank_tft = lastRank_tft;
    }

    public LeagueEntry getLastRankTftSecond() {
      return lastRank_tftSecond;
    }

    public void setLastRankTftSecond(LeagueEntry lastRank_tftSecond) {
      this.lastRank_tftSecond = lastRank_tftSecond;
    }
  }
}
