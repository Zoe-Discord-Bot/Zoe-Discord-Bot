package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.EventListener;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.CleanChannelOption;
import ch.kalunight.zoe.model.config.option.ForceVerificationOption;
import ch.kalunight.zoe.model.config.option.CleanChannelOption.CleanChannelOptionInfo;
import ch.kalunight.zoe.model.config.option.GameInfoCardOption;
import ch.kalunight.zoe.model.config.option.InfoPanelRankedOption;
import ch.kalunight.zoe.model.config.option.RankChannelFilterOption;
import ch.kalunight.zoe.model.config.option.RankRoleOption;
import ch.kalunight.zoe.model.config.option.RankChannelFilterOption.RankChannelFilter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.constant.Platform;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.config.option.RoleOption;
import ch.kalunight.zoe.model.config.option.SelfAddingOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.DTO.ServerRawSettings;

public class ConfigRepository {

  private static final String FALSE_DB = "FALSE";

  private static final String INSERT_INTO_SERVER_CONFIGURATION = "INSERT INTO server_configuration (servConfig_fk_server) "
      + "VALUES (%d)";

  private static final String SELECT_SERVER_CONFIG_WITH_SERV_ID = "SELECT servConfig_id FROM server_configuration "
      + "WHERE servConfig_fk_server = %d";

  private static final String INSERT_INTO_SELF_ADDING_OPTION = "INSERT INTO self_adding_option "
      + "(selfOption_fk_serverConfig, selfOption_activate) VALUES (%d, %s)";

  private static final String INSERT_INTO_REGION_OPTION = "INSERT INTO region_option "
      + "(regionOption_fk_serverConfig) VALUES (%d)";

  private static final String INSERT_INTO_CLEAN_CHANNEL_OPTION = "INSERT INTO clean_channel_option "
      + "(cleanOption_fk_serverConfig, cleanOption_option) VALUES (%d, '%s')";
  
  private static final String INSERT_INTO_RANK_CHANNEL_FILTER_OPTION = "INSERT INTO rank_channel_filter_option "
      + "(rankchannelFilterOption_fk_serverConfig, rankchannelFilterOption_option) VALUES (%d, '%s')";
  
  private static final String INSERT_INTO_RANK_ROLE_OPTION = "INSERT INTO rank_role_option "
      + "(rankRoleOption_fk_serverConfig, rankRoleOption_soloqEnable, rankRoleOption_flexEnable, rankRoleOption_tftEnable) VALUES (%d, '%s', '%s', '%s')";

  private static final String INSERT_INTO_GAME_INFO_CARD_OPTION = "INSERT INTO game_info_card_option "
      + "(gameCardOption_fk_serverConfig, gameCardOption_activate) VALUES (%d, %s)";

  private static final String INSERT_INTO_FORCE_VERIFICATION_OPTION = "INSERT INTO force_verification_option "
      + "(verificationOption_fk_serverConfig, verificationOption_activate) VALUES (%d, %s)";
  
  private static final String INSERT_INTO_ROLE_OPTION = "INSERT INTO role_option "
      + "(roleOption_fk_serverConfig) VALUES (%d)";
  
  private static final String INSERT_INTO_INFOPANEL_RANKED_OPTION = "INSERT INTO info_panel_ranked_option "
      + "(infopanelranked_fk_serverconfig) VALUES (%d)";

  private static final String SELECT_ALL_OPTIONS_SETTINGS = 
      "SELECT " + 
      "self_adding_option.selfoption_activate, " + 
      "role_option.roleoption_roleid, " + 
      "region_option.regionoption_region, " +
      "game_info_card_option.gamecardoption_activate, " + 
      "clean_channel_option.cleanoption_channelid, " + 
      "clean_channel_option.cleanoption_option  " +
      "FROM server " + 
      "INNER JOIN server_configuration ON server.serv_id = server_configuration.servconfig_fk_server " + 
      "INNER JOIN role_option ON server_configuration.servconfig_id = role_option.roleoption_fk_serverconfig " + 
      "INNER JOIN game_info_card_option ON server_configuration.servconfig_id = game_info_card_option.gamecardoption_fk_serverconfig " + 
      "INNER JOIN region_option ON server_configuration.servconfig_id = region_option.regionoption_fk_serverconfig " + 
      "INNER JOIN self_adding_option ON server_configuration.servconfig_id = self_adding_option.selfoption_fk_serverconfig " + 
      "INNER JOIN clean_channel_option ON server_configuration.servconfig_id = clean_channel_option.cleanoption_fk_serverconfig " + 
      "WHERE server.serv_guildid = %d";
  
  private static final String SELECT_SERVCONFIG_WITH_GUILDID = "SELECT " + 
      "server_configuration.servconfig_id, " +
      "server_configuration.servconfig_fk_server " + 
      "FROM server " + 
      "INNER JOIN server_configuration ON server.serv_id = server_configuration.servconfig_fk_server " + 
      "WHERE server_configuration.servconfig_fk_server = %d";
  
  private static final String SELECT_INFOPANEL_RANKED_WITH_GUILDID = "SELECT " + 
      "info_panel_ranked_option.infopanelranked_activate " + 
      "FROM server " + 
      "INNER JOIN server_configuration ON server.serv_id = server_configuration.servconfig_fk_server " + 
      "INNER JOIN info_panel_ranked_option ON server_configuration.servconfig_id = info_panel_ranked_option.infopanelranked_fk_serverconfig " + 
      "WHERE server.serv_guildid = %d";
  
  private static final String SELECT_RANK_CHANNEL_FILTER_WITH_GUILDID = "SELECT " + 
      "rank_channel_filter_option.rankchannelfilteroption_option " + 
      "FROM server " + 
      "INNER JOIN server_configuration ON server.serv_id = server_configuration.servconfig_fk_server " + 
      "INNER JOIN rank_channel_filter_option ON server_configuration.servconfig_id = rank_channel_filter_option.rankchannelfilteroption_fk_serverconfig " + 
      "WHERE server.serv_guildid = %d";
  
  private static final String SELECT_FORCE_VERIFICATION_WITH_GUILD_ID = "SELECT " + 
      "force_verification_option.verificationoption_activate " + 
      "FROM server " + 
      "INNER JOIN server_configuration ON server.serv_id = server_configuration.servconfig_fk_server " + 
      "INNER JOIN force_verification_option ON server_configuration.servconfig_id = force_verification_option.verificationoption_fk_serverconfig " + 
      "WHERE server.serv_guildid = %d";
  
  private static final String SELECT_RANK_ROLE_OPTION_WITH_GUILD_ID = "SELECT " + 
      "rank_role_option.rankroleoption_ironid, " + 
      "rank_role_option.rankroleoption_bronzeid, " + 
      "rank_role_option.rankroleoption_silverid, " + 
      "rank_role_option.rankroleoption_goldid, " + 
      "rank_role_option.rankroleoption_platinumid, " + 
      "rank_role_option.rankroleoption_diamondid, " + 
      "rank_role_option.rankroleoption_masterid, " + 
      "rank_role_option.rankroleoption_grandmasterid, " + 
      "rank_role_option.rankroleoption_challengerid, " + 
      "rank_role_option.rankroleoption_soloqenable, " + 
      "rank_role_option.rankroleoption_flexenable, " + 
      "rank_role_option.rankroleoption_tftenable, " +
      "rank_role_option.rankroleoption_id, " + 
      "rank_role_option.rankroleoption_fk_serverconfig " + 
      "FROM server " + 
      "INNER JOIN server_configuration ON server.serv_id = server_configuration.servconfig_fk_server " + 
      "INNER JOIN rank_role_option ON server_configuration.servconfig_id = rank_role_option.rankroleoption_fk_serverconfig " + 
      "WHERE server.serv_guildid = %d";

  private static final String UPDATE_CLEAN_CHANNEL_OPTION_WITH_GUILD_ID =
      "UPDATE clean_channel_option " +
      "SET cleanOption_option = '%s', cleanOption_channelId = %d " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = clean_channel_option.cleanOption_fk_serverConfig";
  
  private static final String UPDATE_RANK_CHANNEL_FILTER_OPTION_WITH_GUILD_ID =
      "UPDATE rank_channel_filter_option " +
      "SET rankchannelFilterOption_option = '%s' " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = rank_channel_filter_option.rankchannelFilterOption_fk_serverConfig";
  
  private static final String UPDATE_INFOPANEL_RANKED_OPTION_WITH_GUILD_ID =
      "UPDATE info_panel_ranked_option " +
      "SET infoPanelRanked_activate = %s " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = info_panel_ranked_option.infoPanelRanked_fk_serverConfig";
  
  private static final String UPDATE_GAME_INFO_CARD_OPTION_WITH_GUILD_ID =
      "UPDATE game_info_card_option " +
      "SET gameCardOption_activate = %s " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = game_info_card_option.gameCardOption_fk_serverConfig";
  
  private static final String UPDATE_REGION_OPTION_WITH_GUILD_ID =
      "UPDATE region_option " +
      "SET regionOption_region = '%s' " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = region_option.regionOption_fk_serverConfig";
  
  private static final String UPDATE_ROLE_OPTION_WITH_GUILD_ID =
      "UPDATE role_option " +
      "SET roleOption_roleId = %d " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = role_option.roleOption_fk_serverConfig";
  
  private static final String UPDATE_RANK_ROLE_OPTION_ROLE_WITH_GUILD_ID =
      "UPDATE rank_role_option " +
      "SET rankRoleOption_ironId = %d, " +
      "rankRoleOption_bronzeId = %d, " +
      "rankRoleOption_silverId = %d, " +
      "rankRoleOption_goldId = %d, " +
      "rankRoleOption_platinumId = %d, " +
      "rankRoleOption_diamondId = %d, " +
      "rankRoleOption_masterId = %d, " +
      "rankRoleOption_grandMasterId = %d, " +
      "rankRoleOption_challengerId = %d " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = rank_role_option.rankRoleOption_fk_serverConfig";
  
  private static final String UPDATE_RANK_ROLE_OPTION_QUEUE_WITH_GUILD_ID =
      "UPDATE rank_role_option " +
      "SET rankRoleOption_soloqEnable = %s, " +
      "rankRoleOption_flexEnable = %s, " +
      "rankRoleOption_tftEnable = %s " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = rank_role_option.rankRoleOption_fk_serverConfig";
  
  private static final String UPDATE_SELF_ADDING_OPTION_WITH_GUILD_ID =
      "UPDATE self_adding_option " +
      "SET selfOption_activate = %s " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = self_adding_option.selfOption_fk_serverConfig";
  
  private static final String UPDATE_FORCE_VERIFICATION_OPTION_WITH_GUILD_ID =
      "UPDATE force_verification_option " +
      "SET verificationOption_activate = %s " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = force_verification_option.verificationOption_fk_serverConfig";
  

  private static final Logger logger = LoggerFactory.getLogger(ConfigRepository.class);

  private ConfigRepository() {
    //hide default constructor
  }

  public static void initDefaultConfig(Statement statement, long servId) throws SQLException {
    //Create ServerConfiguration
    String finalQuery = String.format(INSERT_INTO_SERVER_CONFIGURATION, servId);
    statement.execute(finalQuery);

    //Get servConfig_id from ServerConfiguration
    finalQuery = String.format(SELECT_SERVER_CONFIG_WITH_SERV_ID, servId);
    ResultSet result = null;
    long servConfigId;
    try {
      result = statement.executeQuery(finalQuery);
      result.next();
      servConfigId = result.getLong("servConfig_id");
    }finally {
      RepoRessources.closeResultSet(result);
    }

    //Create SelfAddingOption
    finalQuery = String.format(INSERT_INTO_SELF_ADDING_OPTION, servConfigId, FALSE_DB);
    statement.execute(finalQuery);

    //Create RegionOption
    finalQuery = String.format(INSERT_INTO_REGION_OPTION, servConfigId);
    statement.execute(finalQuery);

    //Create CleanChannelOption
    finalQuery = String.format(INSERT_INTO_CLEAN_CHANNEL_OPTION, servConfigId, CleanChannelOptionInfo.DISABLE.toString());
    statement.execute(finalQuery);

    //Create GameInfoCardOption
    finalQuery = String.format(INSERT_INTO_GAME_INFO_CARD_OPTION, servConfigId, "TRUE");
    statement.execute(finalQuery);

    //Create RoleOption
    finalQuery = String.format(INSERT_INTO_ROLE_OPTION, servConfigId);
    statement.execute(finalQuery);
    
    //Create InfoPanelRankedOption
    finalQuery = String.format(INSERT_INTO_INFOPANEL_RANKED_OPTION, servConfigId);
    statement.execute(finalQuery);
    
    //Create RankchannelFilterOption
    finalQuery = String.format(INSERT_INTO_RANK_CHANNEL_FILTER_OPTION, servConfigId, RankChannelFilter.ALL.toString());
    statement.execute(finalQuery);
    
    //Create RankRoleOption
    finalQuery = String.format(INSERT_INTO_RANK_ROLE_OPTION, servConfigId, FALSE_DB, FALSE_DB, FALSE_DB);
    statement.execute(finalQuery);
    
    //Create ForceVerificationOption
    finalQuery = String.format(INSERT_INTO_FORCE_VERIFICATION_OPTION, servConfigId, FALSE_DB);
    statement.execute(finalQuery);
  }
  
  public static DTO.ServerConfig getServerConfigDTO(long serverId) throws SQLException{
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
      
      String finalQuery = String.format(SELECT_SERVCONFIG_WITH_GUILDID, serverId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.ServerConfig(result);
    } finally {
      RepoRessources.closeResultSet(result);
    }
  }
  
  public static void createInfoPanelRankedOption(long servConfigId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {
      
      String finalQuery = String.format(INSERT_INTO_INFOPANEL_RANKED_OPTION, servConfigId);
      query.execute(finalQuery);
    }
  }

  public static ServerConfiguration getServerConfiguration(long guildId, JDA jda) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {

      DTO.ServerRawSettings settings = getAllSettingsDTO(guildId, conn);
      
      if(settings == null) {
        logger.warn("Server id {} has missed configuration!", guildId);
        return null;
      }
      
      SelfAddingOption selfAddingOption = new SelfAddingOption(guildId);
      selfAddingOption.setOptionActivated(settings.selfoption_activate);

      RegionOption regionOption = new RegionOption(guildId);
      if(settings.regionoption_region != null) {
        regionOption.setRegion(settings.regionoption_region);
      }

      CleanChannelOption cleanChannelOption = getCleanChannelOption(guildId, settings, jda, conn);

      GameInfoCardOption infoCardOption = new GameInfoCardOption(guildId);
      infoCardOption.setOptionActivated(settings.gamecardoption_activate);

      RoleOption roleOption = getRoleOption(settings, guildId, jda, conn);

      Boolean infopanelRankedOptionActivate = getInfoPanelRankedState(guildId, conn);
      InfoPanelRankedOption infopanelRankedOption = new InfoPanelRankedOption(guildId);
      if(infopanelRankedOptionActivate == null) {
        infopanelRankedOption.setOptionActivated(true);
        //Create InfoPanelRankedOption
        createInfoPanelRankedOptionTable(conn.createStatement(), guildId, conn);
      }else {
        infopanelRankedOption.setOptionActivated(infopanelRankedOptionActivate);
      }
      
      RankChannelFilter rankchannelFilter = getRankChannelFilterOption(guildId, conn);
      RankChannelFilterOption rankchannelFilterOption = new RankChannelFilterOption(guildId);
      if(rankchannelFilter == null) {
        rankchannelFilterOption.setRankChannelFilter(rankchannelFilter);
        //Create RankchannelFilterOption
        createRankchannelFilterOption(conn.createStatement(), guildId, RankChannelFilter.ALL, conn);
      }else {
        rankchannelFilterOption.setRankChannelFilter(rankchannelFilter);
      }
      
      RankRoleOption rankRoleOption = getRankRoleOption(guildId, conn, jda.getGuildById(guildId));
      if(rankRoleOption == null) {
        //Create RankRoleOption
        createRankRoleOption(conn.createStatement(), guildId, conn);
        
        rankRoleOption = new RankRoleOption(guildId);
      }
      
      ForceVerificationOption forceVericationOption = new ForceVerificationOption(guildId);
      Boolean verificationOption = getForceVerificationOption(conn, guildId);
      if(verificationOption == null) {
        createForceVerificationOption(conn, guildId, conn.createStatement());
        forceVericationOption.setOptionActivated(false);
      }else {
        forceVericationOption.setOptionActivated(verificationOption);
      }
      
      ServerConfiguration serverConfig = new ServerConfiguration(guildId);
      serverConfig.setUserSelfAdding(selfAddingOption);
      serverConfig.setDefaultRegion(regionOption);
      serverConfig.setCleanChannelOption(cleanChannelOption);
      serverConfig.setInfoCardsOption(infoCardOption);
      serverConfig.setZoeRoleOption(roleOption);
      serverConfig.setInfopanelRankedOption(infopanelRankedOption);
      serverConfig.setRankchannelFilterOption(rankchannelFilterOption);
      serverConfig.setRankRoleOption(rankRoleOption);
      serverConfig.setForceVerificationOption(forceVericationOption);
      
      return serverConfig;
    }
  }
  
  private static void createForceVerificationOption(Connection conn, long guildId, Statement statement) throws SQLException {
    Server server = ServerRepository.getServerWithGuildId(guildId, conn);

    //Get servConfig_id from ServerConfiguration
    String finalQuery = String.format(SELECT_SERVER_CONFIG_WITH_SERV_ID, server.serv_id);
    ResultSet result = null;
    long servConfigId;
    try {
      result = statement.executeQuery(finalQuery);
      result.next();
      servConfigId = result.getLong("servConfig_id");
    }finally {
      RepoRessources.closeResultSet(result);
    }
    
    finalQuery = String.format(INSERT_INTO_FORCE_VERIFICATION_OPTION, servConfigId, FALSE_DB);
    statement.execute(finalQuery);
  }

  private static Boolean getForceVerificationOption(Connection conn, long guildId) throws SQLException {
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);){
      //Get All Options
      String finalQuery = String.format(SELECT_FORCE_VERIFICATION_WITH_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return result.getBoolean("verificationOption_activate");
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  private static RankRoleOption getRankRoleOption(long guildId, Connection conn, Guild guild) throws SQLException {
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_RANK_ROLE_OPTION_WITH_GUILD_ID, guildId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      
      DTO.RankRoleOption roleOptionDB = new DTO.RankRoleOption(result);
      RankRoleOption roleOption = new RankRoleOption(guildId);
      roleOption.setValues(roleOptionDB, guild);
      
      return roleOption;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  @Nullable
  private static DTO.ServerRawSettings getAllSettingsDTO(long guildId, Connection conn) throws SQLException {
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);){
      //Get All Options
      String finalQuery = String.format(SELECT_ALL_OPTIONS_SETTINGS, guildId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      return new DTO.ServerRawSettings(result);
    }finally {
      RepoRessources.closeResultSet(result);
    }

  }

  private static void createInfoPanelRankedOptionTable(Statement statement, long guildId, Connection conn) throws SQLException {
    
    Server server = ServerRepository.getServerWithGuildId(guildId, conn);

    //Get servConfig_id from ServerConfiguration
    String finalQuery = String.format(SELECT_SERVER_CONFIG_WITH_SERV_ID, server.serv_id);
    ResultSet result = null;
    long servConfigId;
    try {
      result = statement.executeQuery(finalQuery);
      result.next();
      servConfigId = result.getLong("servConfig_id");
    }finally {
      RepoRessources.closeResultSet(result);
    }
    
    finalQuery = String.format(INSERT_INTO_INFOPANEL_RANKED_OPTION, servConfigId);
    statement.execute(finalQuery);
  }

  private static void createRankchannelFilterOption(Statement statement, long guildId, RankChannelFilter rankFilter, Connection conn) throws SQLException {
    
    Server server = ServerRepository.getServerWithGuildId(guildId, conn);

    //Get servConfig_id from ServerConfiguration
    String finalQuery = String.format(SELECT_SERVER_CONFIG_WITH_SERV_ID, server.serv_id);
    ResultSet result = null;
    long servConfigId;
    try {
      result = statement.executeQuery(finalQuery);
      result.next();
      servConfigId = result.getLong("servConfig_id");
    }finally {
      RepoRessources.closeResultSet(result);
    }
    
    finalQuery = String.format(INSERT_INTO_RANK_CHANNEL_FILTER_OPTION, servConfigId, rankFilter.toString());
    statement.execute(finalQuery);
  }
  
  private static void createRankRoleOption(Statement statement, long guildId, Connection conn) throws SQLException {
    
    Server server = ServerRepository.getServerWithGuildId(guildId, conn);

    //Get servConfig_id from ServerConfiguration
    String finalQuery = String.format(SELECT_SERVER_CONFIG_WITH_SERV_ID, server.serv_id);
    ResultSet result = null;
    long servConfigId;
    try {
      result = statement.executeQuery(finalQuery);
      result.next();
      servConfigId = result.getLong("servConfig_id");
    }finally {
      RepoRessources.closeResultSet(result);
    }
    
    finalQuery = String.format(INSERT_INTO_RANK_ROLE_OPTION, servConfigId, FALSE_DB, FALSE_DB, FALSE_DB);
    statement.execute(finalQuery);
  }
  
  private static RoleOption getRoleOption(ServerRawSettings settings, long guildId, JDA jda, Connection conn) throws SQLException {
    RoleOption roleOption = new RoleOption(guildId);
    long roleId = settings.roleoption_roleid;

    if(roleId == 0) {
      return roleOption;
    }
    
    try {
      Role role = jda.getGuildById(guildId).getRoleById(roleId);
      roleOption.setRole(role);
      if(role == null) {
        logger.info("Zoe role has been deleted. We update the db.");
        updateRoleOption(guildId, 0, jda, conn);
        
        DTO.InfoChannel infoChannelDb = InfoChannelRepository.getInfoChannel(guildId, conn);
        TextChannel infoChannel = jda.getTextChannelById(infoChannelDb.infochannel_channelid);
        
        if(infoChannel != null) {
          Role everyone = infoChannel.getGuild().getPublicRole();
          infoChannel.putPermissionOverride(everyone).clear(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).queue();
        }
        
        DTO.RankHistoryChannel rankChannelDb = RankHistoryChannelRepository.getRankHistoryChannel(guildId, conn);
        TextChannel rankChannel = jda.getTextChannelById(rankChannelDb.rhChannel_channelId);
        
        if(rankChannel != null) {
          Role everyone = rankChannel.getGuild().getPublicRole();
          rankChannel.putPermissionOverride(everyone).clear(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).queue();
        }
      }
    }catch(NullPointerException e) {
      logger.warn("A guild has been detected like non existant. ID of the guild : {}", guildId);
      throw e;
    }
    return roleOption;
  }

  private static CleanChannelOption getCleanChannelOption(long guildId, ServerRawSettings settings, JDA jda, Connection conn) throws SQLException {
    CleanChannelOption cleanChannelOption = new CleanChannelOption(guildId);
    cleanChannelOption.setCleanChannelOption(settings.cleanoption_option);

    if(!cleanChannelOption.getCleanChannelOption().equals(CleanChannelOptionInfo.DISABLE)) {
      long cleanChannelId = settings.cleanoption_channelid;
      try {
        TextChannel cleanChannel = jda.getGuildById(guildId).getTextChannelById(cleanChannelId);
        if(cleanChannel == null) {
          logger.info("clean channel has been deleted. We update the db.");
          updateCleanChannelOption(guildId, 0, CleanChannelOptionInfo.DISABLE.toString(), jda, conn);
          cleanChannelOption.setCleanChannelOption(CleanChannelOptionInfo.DISABLE);
        }
        cleanChannelOption.setCleanChannel(cleanChannel);
      }catch(NullPointerException e) {
        logger.warn("A guild has been detected like non existant. Id of the guild : {}", guildId);
        throw e;
      }
    }
    return cleanChannelOption;
  }
  
  public static Boolean getInfoPanelRankedState(long guildId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      return getInfoPanelRankedState(guildId, conn);
    }
}
  
  public static Boolean getInfoPanelRankedState(long guildId, Connection conn) throws SQLException {
      ResultSet result = null;
      try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

        String finalQuery = String.format(SELECT_INFOPANEL_RANKED_WITH_GUILDID, guildId);
        result = query.executeQuery(finalQuery);
        int rowCount = result.last() ? result.getRow() : 0;
        if(rowCount == 0) {
          return null;
        }
        
        return result.getBoolean("infopanelranked_activate");
      }finally {
        RepoRessources.closeResultSet(result);
      }
  }
  
  public static RankChannelFilter getRankChannelFilterOption(long guildId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      return getRankChannelFilterOption(guildId, conn);
    }
}
  
  public static RankChannelFilter getRankChannelFilterOption(long guildId, Connection conn) throws SQLException {
    ResultSet result = null;
    try (Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

      String finalQuery = String.format(SELECT_RANK_CHANNEL_FILTER_WITH_GUILDID, guildId);
      result = query.executeQuery(finalQuery);
      int rowCount = result.last() ? result.getRow() : 0;
      if(rowCount == 0) {
        return null;
      }
      
      return RankChannelFilter.valueOf(result.getString("rankchannelFilterOption_option"));
    }finally {
      RepoRessources.closeResultSet(result);
    }
}
  
  public static void updateInfoPanelRanked(long guildID, boolean activate, JDA jda) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_INFOPANEL_RANKED_OPTION_WITH_GUILD_ID, activate, guildID);
      query.executeUpdate(finalQuery);
      
      EventListener.getServersConfig().put(guildID, ConfigRepository.getServerConfiguration(guildID, jda));
    }
  }
  
  public static void updateCleanChannelOption(long guildId, long cleanOptionChannelId, String cleanChannelOptionMode, JDA jda) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      updateCleanChannelOption(guildId, cleanOptionChannelId, cleanChannelOptionMode, jda, conn);
    }
  }

  public static void updateCleanChannelOption(long guildId, long cleanOptionChannelId, String cleanChannelOptionMode, JDA jda, Connection conn) throws SQLException {
    try (Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_CLEAN_CHANNEL_OPTION_WITH_GUILD_ID, 
          cleanChannelOptionMode, cleanOptionChannelId, guildId);
      query.executeUpdate(finalQuery);
      
      EventListener.getServersConfig().put(guildId, ConfigRepository.getServerConfiguration(guildId, jda));
    }
  }
 
  public static void updateGameInfoCardOption(long guildId, boolean activate, JDA jda)
      throws SQLException {

    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_GAME_INFO_CARD_OPTION_WITH_GUILD_ID, 
          activate, guildId);
      query.executeUpdate(finalQuery);
      
      EventListener.getServersConfig().put(guildId, ConfigRepository.getServerConfiguration(guildId, jda));
    }
  }
  
  public static void updateRankchannelFilter(long guildId, RankChannelFilter rankchannelFilter, JDA jda)
      throws SQLException {

    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_RANK_CHANNEL_FILTER_OPTION_WITH_GUILD_ID, 
          rankchannelFilter.toString(), guildId);
      query.executeUpdate(finalQuery);
      
      EventListener.getServersConfig().put(guildId, ConfigRepository.getServerConfiguration(guildId, jda));
    }
  }
  
  public static void updateRegionOption(long guildId, Platform platform, JDA jda)
      throws SQLException {

    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String platformString = "";
      
      if(platform != null) {
        platformString = platform.name();
      }
      
      String finalQuery = String.format(UPDATE_REGION_OPTION_WITH_GUILD_ID, 
          platformString, guildId);
      query.executeUpdate(finalQuery);
      
      EventListener.getServersConfig().put(guildId, ConfigRepository.getServerConfiguration(guildId, jda));
    }
  }
  
  public static void updateForceVerificationOption(long guildId, JDA jda, Connection conn, boolean activate) throws SQLException {
    try (Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_FORCE_VERIFICATION_OPTION_WITH_GUILD_ID, activate, guildId);
      query.executeUpdate(finalQuery);
      
      EventListener.getServersConfig().put(guildId, ConfigRepository.getServerConfiguration(guildId, jda));
    }
  }
  
  public static void updateForceVerificationOption(long guildId, JDA jda, boolean activate) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      updateForceVerificationOption(guildId, jda, conn, activate);
    }
  }
  
  public static void updateRankRoleOption(long guildId, JDA jda, Connection conn, long ironId, long bronzeId, long silverId, long goldId, 
      long platinumId, long diamondId, long masterId, long grandMasterId, long challegerId) throws SQLException {
    try (Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_RANK_ROLE_OPTION_ROLE_WITH_GUILD_ID, 
          ironId, bronzeId, silverId, goldId, platinumId, diamondId, masterId, grandMasterId, challegerId, guildId);
      query.executeUpdate(finalQuery);
      
      EventListener.getServersConfig().put(guildId, ConfigRepository.getServerConfiguration(guildId, jda));
    }
  }
  
  public static void updateRankRoleOption(long guildId, JDA jda, long ironId, long bronzeId, long silverId, long goldId, 
      long platinumId, long diamondId, long masterId, long grandMasterId, long challegerId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      updateRankRoleOption(guildId, jda, conn, ironId, bronzeId, silverId, goldId, platinumId, diamondId, masterId, grandMasterId, challegerId);
    }
  }
  
  public static void updateRankRoleOption(long guildId, JDA jda, Connection conn, boolean soloqEnable, boolean flexEnable, boolean tftEnable) throws SQLException {
    try (Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_RANK_ROLE_OPTION_QUEUE_WITH_GUILD_ID, 
          soloqEnable, flexEnable, tftEnable, guildId);
      query.executeUpdate(finalQuery);
      
      EventListener.getServersConfig().put(guildId, ConfigRepository.getServerConfiguration(guildId, jda));
    }
  }
  
  public static void updateRankRoleOption(long guildId, JDA jda, boolean soloqEnable, boolean flexEnable, boolean tftEnable) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      updateRankRoleOption(guildId, jda, conn, soloqEnable, flexEnable, tftEnable);
    }
  }
  
  public static void updateRoleOption(long guildId, long roleId, JDA jda) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();) {
      updateRoleOption(guildId, roleId, jda, conn);
    }
  }
  
  public static void updateRoleOption(long guildId, long roleId, JDA jda, Connection conn) throws SQLException {
    try (Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_ROLE_OPTION_WITH_GUILD_ID, 
          roleId, guildId);
      query.executeUpdate(finalQuery);
      
      EventListener.getServersConfig().put(guildId, ConfigRepository.getServerConfiguration(guildId, jda));
    }
  }

  public static void updateSelfAddingOption(long guildId, boolean activate, JDA jda) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_SELF_ADDING_OPTION_WITH_GUILD_ID, 
          activate, guildId);
      query.executeUpdate(finalQuery);
      
      EventListener.getServersConfig().put(guildId, ConfigRepository.getServerConfiguration(guildId, jda));
    }
  }

}
