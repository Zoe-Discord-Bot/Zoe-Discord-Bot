package ch.kalunight.zoe.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.CleanChannelOption;
import ch.kalunight.zoe.model.config.option.CleanChannelOption.CleanChannelOptionInfo;
import ch.kalunight.zoe.model.config.option.GameInfoCardOption;
import ch.kalunight.zoe.model.config.option.InfoPanelRankedOption;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.constant.Platform;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.config.option.RoleOption;
import ch.kalunight.zoe.model.config.option.SelfAddingOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;

public class ConfigRepository {

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

  private static final String INSERT_INTO_GAME_INFO_CARD_OPTION = "INSERT INTO game_info_card_option "
      + "(gameCardOption_fk_serverConfig, gameCardOption_activate) VALUES (%d, %s)";

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
      "clean_channel_option.cleanoption_option " + 
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

  private static final String UPDATE_CLEAN_CHANNEL_OPTION_WITH_GUILD_ID =
      "UPDATE clean_channel_option " +
      "SET cleanOption_option = '%s', cleanOption_channelId = %d " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = clean_channel_option.cleanOption_fk_serverConfig";
  
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
  
  private static final String UPDATE_SELF_ADDING_OPTION_WITH_GUILD_ID =
      "UPDATE self_adding_option " +
      "SET selfOption_activate = %s " +
      "FROM server, server_configuration " +
      "WHERE server.serv_guildId = %d AND " +
      "server.serv_id = server_configuration.servConfig_fk_server AND " +
      "server_configuration.servConfig_id = self_adding_option.selfOption_fk_serverConfig";
  

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
    finalQuery = String.format(INSERT_INTO_SELF_ADDING_OPTION, servConfigId, "FALSE");
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

  public static ServerConfiguration getServerConfiguration(long guildId) throws SQLException {
    ResultSet result = null;
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      //Get All Options
      String finalQuery = String.format(SELECT_ALL_OPTIONS_SETTINGS, guildId);
      result = query.executeQuery(finalQuery);
      result.next();

      SelfAddingOption selfAddingOption = new SelfAddingOption(guildId);
      selfAddingOption.setOptionActivated(result.getBoolean("selfOption_activate"));

      RegionOption regionOption = new RegionOption(guildId);
      String region = result.getString("regionOption_region");
      if(region != null && !region.equals("")) {
        regionOption.setRegion(Platform.valueOf(region));
      }

      CleanChannelOption cleanChannelOption = getCleanChannelOption(guildId, result);

      GameInfoCardOption infoCardOption = new GameInfoCardOption(guildId);
      infoCardOption.setOptionActivated(result.getBoolean("gameCardOption_activate"));

      RoleOption roleOption = getRoleOption(result, guildId);

      Boolean infopanelRankedOptionActivate = getInfoPanelRankedState(guildId);
      InfoPanelRankedOption infopanelRankedOption = new InfoPanelRankedOption(guildId);
      if(infopanelRankedOptionActivate == null) {
        infopanelRankedOption.setOptionActivated(true);
        //Create InfoPanelRankedOption
        createInfoPanelRankedOptionTable(conn.createStatement(), guildId);
      }else {
        infopanelRankedOption.setOptionActivated(infopanelRankedOptionActivate);
      }
      
      ServerConfiguration serverConfig = new ServerConfiguration(guildId);
      serverConfig.setUserSelfAdding(selfAddingOption);
      serverConfig.setDefaultRegion(regionOption);
      serverConfig.setCleanChannelOption(cleanChannelOption);
      serverConfig.setInfoCardsOption(infoCardOption);
      serverConfig.setZoeRoleOption(roleOption);
      serverConfig.setInfopanelRankedOption(infopanelRankedOption);
      
      return serverConfig;
    }finally {
      RepoRessources.closeResultSet(result);
    }
  }

  private static void createInfoPanelRankedOptionTable(Statement statement, long guildId) throws SQLException {
    
    Server server = ServerRepository.getServer(guildId);

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

  private static RoleOption getRoleOption(ResultSet result, long guildId) throws SQLException {
    RoleOption roleOption = new RoleOption(guildId);
    long roleId = result.getLong("roleOption_roleId");

    if(roleId == 0) {
      return roleOption;
    }
    
    try {
      Role role = Zoe.getJda().getGuildById(guildId).getRoleById(roleId);
      roleOption.setRole(role);
      if(role == null) {
        logger.info("Zoe role has been deleted. We update the db.");
        updateRoleOption(guildId, 0);
        
        DTO.InfoChannel infoChannelDb = InfoChannelRepository.getInfoChannel(guildId);
        TextChannel infoChannel = Zoe.getJda().getTextChannelById(infoChannelDb.infochannel_channelid);
        
        if(infoChannel != null) {
          Role everyone = infoChannel.getGuild().getPublicRole();
          infoChannel.putPermissionOverride(everyone).clear(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).queue();
        }
        
        DTO.RankHistoryChannel rankChannelDb = RankHistoryChannelRepository.getRankHistoryChannel(guildId);
        TextChannel rankChannel = Zoe.getJda().getTextChannelById(rankChannelDb.rhChannel_channelId);
        
        if(rankChannel != null) {
          Role everyone = rankChannel.getGuild().getPublicRole();
          rankChannel.putPermissionOverride(everyone).clear(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).queue();
        }
      }
    }catch(NullPointerException e) {
      logger.warn("A guild has been detected like non existant. Will be deleted from DB now.");
      ServerRepository.deleteServer(guildId);
      throw e;
    }
    return roleOption;
  }

  private static CleanChannelOption getCleanChannelOption(long guildId, ResultSet result) throws SQLException {
    CleanChannelOption cleanChannelOption = new CleanChannelOption(guildId);
    cleanChannelOption.setCleanChannelOption(CleanChannelOptionInfo.valueOf(result.getString("cleanOption_option")));

    if(!cleanChannelOption.getCleanChannelOption().equals(CleanChannelOptionInfo.DISABLE)) {
      long cleanChannelId = result.getLong("cleanOption_channelId");
      try {
        TextChannel cleanChannel = Zoe.getJda().getGuildById(guildId).getTextChannelById(cleanChannelId);
        if(cleanChannel == null) {
          logger.info("clean channel has been deleted. We update the db.");
          updateCleanChannelOption(guildId, 0, CleanChannelOptionInfo.DISABLE.toString());
          cleanChannelOption.setCleanChannelOption(CleanChannelOptionInfo.DISABLE);
        }
        cleanChannelOption.setCleanChannel(cleanChannel);
      }catch(NullPointerException e) {
        logger.warn("A guild has been detected like non existant. Will be deleted from DB now.");
        ServerRepository.deleteServer(guildId);
        throw e;
      }
    }
    return cleanChannelOption;
  }
  
  public static Boolean getInfoPanelRankedState(long guildId) throws SQLException {
      ResultSet result = null;
      try (Connection conn = RepoRessources.getConnection();
          Statement query = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

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
  
  public static void updateInfoPanelRanked(long guildID, boolean activate) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_INFOPANEL_RANKED_OPTION_WITH_GUILD_ID, activate, guildID);
      query.executeUpdate(finalQuery);
    }
  }

  public static void updateCleanChannelOption(long guildId, long cleanOptionChannelId, String cleanChannelOptionMode)
      throws SQLException {

    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_CLEAN_CHANNEL_OPTION_WITH_GUILD_ID, 
          cleanChannelOptionMode, cleanOptionChannelId, guildId);
      query.executeUpdate(finalQuery);
    }
  }
 
  
  public static void updateGameInfoCardOption(long guildId, boolean activate)
      throws SQLException {

    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_GAME_INFO_CARD_OPTION_WITH_GUILD_ID, 
          activate, guildId);
      query.executeUpdate(finalQuery);
    }
  }
  
  public static void updateRegionOption(long guildId, Platform platform)
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
    }
  }
  

  public static void updateRoleOption(long guildId, long roleId) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_ROLE_OPTION_WITH_GUILD_ID, 
          roleId, guildId);
      query.executeUpdate(finalQuery);
    }
  }

  public static void updateSelfAddingOption(long guildId, boolean activate) throws SQLException {
    try (Connection conn = RepoRessources.getConnection();
        Statement query = conn.createStatement();) {

      String finalQuery = String.format(UPDATE_SELF_ADDING_OPTION_WITH_GUILD_ID, 
          activate, guildId);
      query.executeUpdate(finalQuery);
    }
  }

}
