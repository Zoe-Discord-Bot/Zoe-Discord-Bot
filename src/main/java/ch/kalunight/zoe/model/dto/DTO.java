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
import com.google.gson.reflect.TypeToken;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.service.analysis.ChampionRole;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.clash.dto.ClashTournament;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.endpoints.tft_league.dto.TFTLeagueEntry;
import net.rithms.riot.api.endpoints.tft_summoner.dto.TFTSummoner;
import net.rithms.riot.constant.Platform;

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

    public User getUser(JDA jda) {
      User user = jda.retrieveUserById(player_discordId, false).complete();
      if(user != null) {
        return user;
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
    public final Platform leagueAccount_server;
    private SavedSummoner summoner = null;

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

    public LeagueAccount(Summoner summoner, TFTSummoner tftSummoner, Platform platform) {
      leagueAccount_name = summoner.getName();
      leagueAccount_summonerId = summoner.getId();
      leagueAccount_accoundId = summoner.getAccountId();
      leagueAccount_puuid = summoner.getPuuid();
      leagueAccount_server = platform;
      leagueAccount_tftSummonerId = tftSummoner.getId();
      leagueAccount_tftAccountId = tftSummoner.getAccountId();
      leagueAccount_tftPuuid = tftSummoner.getPuuid();
      leagueAccount_id = null;
      leagueAccount_fk_player = null;
      leagueAccount_fk_gamecard = null;
      leagueAccount_fk_currentgame = null;

      this.summoner = new SavedSummoner(summoner);
    }

    public SavedSummoner getSummoner() throws RiotApiException {
      return getSummoner(false);
    }

    public SavedSummoner getSummoner(boolean forceRefreshCache) throws RiotApiException {
      if(summoner != null && !forceRefreshCache) {
        return summoner;
      }

      SummonerCache summonerDB = Zoe.getRiotApi().getSummonerWithRateLimit(leagueAccount_server, leagueAccount_summonerId, forceRefreshCache);
      if(summonerDB != null) {
        summoner = summonerDB.sumCache_data;
      }
      return summoner;
    }

    @Override
    public String toString() {
      return "[LeagueAccount: leagueAccount_id : " + leagueAccount_id + "]";
    }
  }

  public static class CurrentGameInfo {
    public final long currentgame_id;
    public final net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo currentgame_currentgame;
    public final Platform currentgame_server;
    public final Long currentgame_gameid;

    public CurrentGameInfo(ResultSet baseData) throws SQLException {
      currentgame_id = baseData.getLong("currentgame_id");
      if(baseData.getString("currentgame_currentgame") != null) {
        currentgame_currentgame = gson.fromJson(baseData.getString("currentgame_currentgame"),
            net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo.class);
      }else {
        currentgame_currentgame = null;
      }

      if(baseData.getString("currentgame_server") != null) {
        currentgame_server = Platform.getPlatformByName(baseData.getString("currentgame_server"));
      }else {
        currentgame_server = null;
      }

      if(baseData.getString("currentgame_gameid") != null) {
        currentgame_gameid = baseData.getLong("currentgame_gameid");
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

  public static class MatchCache {
    public final long mCatch_id;
    public final String mCatch_gameId;
    public final Platform mCatch_platform;
    public final SavedMatch mCatch_savedMatch;
    public final LocalDateTime mCatch_creationTime;

    public MatchCache(ResultSet baseData) throws SQLException {
      mCatch_id = baseData.getLong("mCatch_id");
      mCatch_gameId = baseData.getString("mCatch_gameId");
      mCatch_platform = Platform.getPlatformByName(baseData.getString("mCatch_platform"));
      mCatch_savedMatch = gson.fromJson(baseData.getString("mCatch_savedMatch"), SavedMatch.class);
      mCatch_creationTime = LocalDateTime.parse(baseData.getString("mCatch_creationTime"), DB_TIME_PATTERN);
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
    public final Platform banAcc_server;

    public BannedAccount(ResultSet baseData) throws SQLException {
      banAcc_id = baseData.getLong("banAcc_id");
      banAcc_summonerId = baseData.getString("banAcc_summonerId");
      banAcc_server = Platform.getPlatformByName(baseData.getString("banAcc_server"));
    }

  }

  public static class SummonerCache {
    public final long sumCache_id;
    public final String sumCache_summonerId;
    public final Platform sumCache_server;
    private SavedSummoner sumCache_data;
    public final LocalDateTime sumCache_lastRefresh;

    public SummonerCache(ResultSet baseData) throws SQLException {
      sumCache_id = baseData.getLong("sumCache_id");
      sumCache_summonerId = baseData.getString("sumCache_summonerId");
      sumCache_server = Platform.getPlatformByName(baseData.getString("sumCache_server"));
      this.sumCache_data = gson.fromJson(baseData.getString("sumCache_data"), SavedSummoner.class);
      sumCache_lastRefresh = LocalDateTime.parse(baseData.getString("sumCache_lastRefresh"), DB_TIME_PATTERN);
    }

    public SavedSummoner getSumCacheData() {
      return sumCache_data;
    }

    public void setSumCacheData(SavedSummoner sumCache_data) {
      this.sumCache_data = sumCache_data;
    }

  }

  public static class ChampionMasteryCache {
    public final long champMasCache_id;
    public final String champMasCache_summonerId;
    public final Platform champMasCache_server;
    public final SavedChampionsMastery champMasCache_data;
    public final LocalDateTime champMasCache_lastRefresh;

    public ChampionMasteryCache(ResultSet baseData) throws SQLException {
      champMasCache_id = baseData.getLong("champMasCache_id");
      champMasCache_summonerId = baseData.getString("champMasCache_summonerId");
      champMasCache_server = Platform.getPlatformByName(baseData.getString("champMasCache_server"));
      champMasCache_data = gson.fromJson(baseData.getString("champMasCache_data"), SavedChampionsMastery.class);
      champMasCache_lastRefresh = LocalDateTime.parse(baseData.getString("champMasCache_lastRefresh"), DB_TIME_PATTERN);
    }
  }

  public static class ClashTournamentCache {
    public final long clashTourCache_id;
    public final Platform clashTourCache_server;
    public final List<ClashTournament> clashTourCache_data;
    public final LocalDateTime clashTourCache_lastRefresh;

    public ClashTournamentCache(ResultSet baseData) throws SQLException {
      clashTourCache_id = baseData.getLong("clashTourCache_id");
      clashTourCache_server = Platform.getPlatformByName(baseData.getString("clashTourCache_server"));
      clashTourCache_data = gson.fromJson(baseData.getString("clashTourCache_data"), new TypeToken<List<ClashTournament>>(){}.getType());
      clashTourCache_lastRefresh = LocalDateTime.parse(baseData.getString("clashTourCache_lastRefresh"), DB_TIME_PATTERN);
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
    private TFTLeagueEntry lastRank_tft;
    private TFTLeagueEntry lastRank_tftSecond;
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
        lastRank_tft = gson.fromJson(lastRank, TFTLeagueEntry.class);
      }else {
        lastRank_tft = null;
      }

      lastRank = baseData.getString("lastRank_tftSecond");
      if(lastRank != null) {
        lastRank_tftSecond = gson.fromJson(lastRank, TFTLeagueEntry.class);
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

    public TFTLeagueEntry getLastRankTft() {
      return lastRank_tft;
    }

    public void setLastRankTft(TFTLeagueEntry lastRank_tft) {
      this.lastRank_tft = lastRank_tft;
    }

    public TFTLeagueEntry getLastRankTftSecond() {
      return lastRank_tftSecond;
    }

    public void setLastRankTftSecond(TFTLeagueEntry lastRank_tftSecond) {
      this.lastRank_tftSecond = lastRank_tftSecond;
    }
  }
}
