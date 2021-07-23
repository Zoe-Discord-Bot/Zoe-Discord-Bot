package ch.kalunight.zoe.model.config;

import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.model.config.option.ConfigurationOption;
import ch.kalunight.zoe.model.config.option.ForceVerificationOption;
import ch.kalunight.zoe.model.config.option.GameInfoCardOption;
import ch.kalunight.zoe.model.config.option.InfoPanelRankedOption;
import ch.kalunight.zoe.model.config.option.RankChannelFilterOption;
import ch.kalunight.zoe.model.config.option.RankRoleOption;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.config.option.CleanChannelOption;
import ch.kalunight.zoe.model.config.option.RoleOption;
import ch.kalunight.zoe.model.config.option.SelfAddingOption;

public class ServerConfiguration {
  
  /**
   * Define if user can add them self in the system.
   */
  private SelfAddingOption userSelfAdding;
  
  /**
   * Default region of the server. If a account/player is created without the region and this option is activated the default region will be used.
   */
  private RegionOption defaultRegion;
  
  /**
   * This option hide channels of zoe to non-register players.
   */
  private RoleOption zoeRoleOption;
  
  /**
   * This option activate the system of automatically send infocards in infochannel. Enable as default.
   */
  private GameInfoCardOption infoCardsOption;
  
  /**
   * This option let config a "Clean Channel"
   */
  private CleanChannelOption cleanChannelOption;
  
  /**
   * This option let config the data of infopanel
   */
  private InfoPanelRankedOption infopanelRankedOption;
  
  /**
   * This option let config a filter on the rankchannel to show only one type of game (ALL/LOL/TFT)
   */
  private RankChannelFilterOption rankchannelFilterOption;
  
  /**
   * This option allow to create role depending of players rank
   */
  private RankRoleOption rankRoleOption;
  
  /**
   * This option allow to force verification of each account registration
   */
  private ForceVerificationOption forceVerificationOption;
  
  /**
   * This option activate the command join/leave for everyone. They can join team joinable by everyone. NOT IMPLEMENTED
   */
  private boolean everyoneCanMoveOfTeam = false;
  
  public ServerConfiguration(long guildId) {
    this.defaultRegion = new RegionOption(guildId);
    this.zoeRoleOption = new RoleOption(guildId);
    this.userSelfAdding = new SelfAddingOption(guildId);
    this.infoCardsOption = new GameInfoCardOption(guildId);
    this.cleanChannelOption = new CleanChannelOption(guildId);
    this.infopanelRankedOption = new InfoPanelRankedOption(guildId);
    this.rankchannelFilterOption = new RankChannelFilterOption(guildId);
    this.rankRoleOption = new RankRoleOption(guildId);
    this.forceVerificationOption = new ForceVerificationOption(guildId);
    this.everyoneCanMoveOfTeam = false;
  }

  public List<ConfigurationOption> getAllConfigurationOption() {
    List<ConfigurationOption> options = new ArrayList<>();
    options.add(defaultRegion);
    options.add(zoeRoleOption);
    options.add(cleanChannelOption);
    options.add(userSelfAdding);
    options.add(infoCardsOption);
    options.add(infopanelRankedOption);
    options.add(rankchannelFilterOption);
    options.add(rankRoleOption);
    options.add(forceVerificationOption);
    return options;
  }
  
  public boolean isOptionRequireRefresh() {
    return rankRoleOption.isOptionEnable();
  }
  
  public ForceVerificationOption getForceVerificationOption() {
    return forceVerificationOption;
  }

  public void setForceVerificationOption(ForceVerificationOption forceVerificationOption) {
    this.forceVerificationOption = forceVerificationOption;
  }

  public RankRoleOption getRankRoleOption() {
    return rankRoleOption;
  }

  public void setRankRoleOption(RankRoleOption rankRoleOption) {
    this.rankRoleOption = rankRoleOption;
  }

  public RankChannelFilterOption getRankchannelFilterOption() {
    return rankchannelFilterOption;
  }

  public void setRankchannelFilterOption(RankChannelFilterOption rankchannelFilterOption) {
    this.rankchannelFilterOption = rankchannelFilterOption;
  }

  public RegionOption getDefaultRegion() {
    return defaultRegion;
  }
  
  public RoleOption getZoeRoleOption() {
    return zoeRoleOption;
  }

  public boolean isEveryoneCanMoveOfTeam() {
    return everyoneCanMoveOfTeam;
  }
  
  public SelfAddingOption getUserSelfAdding() {
    return userSelfAdding;
  }

  public GameInfoCardOption getInfoCardsOption() {
    return infoCardsOption;
  }

  public CleanChannelOption getCleanChannelOption() {
    return cleanChannelOption;
  }

  public void setUserSelfAdding(SelfAddingOption userSelfAdding) {
    this.userSelfAdding = userSelfAdding;
  }

  public void setDefaultRegion(RegionOption defaultRegion) {
    this.defaultRegion = defaultRegion;
  }

  public void setZoeRoleOption(RoleOption zoeRoleOption) {
    this.zoeRoleOption = zoeRoleOption;
  }

  public void setInfoCardsOption(GameInfoCardOption infoCardsOption) {
    this.infoCardsOption = infoCardsOption;
  }

  public void setCleanChannelOption(CleanChannelOption cleanChannelOption) {
    this.cleanChannelOption = cleanChannelOption;
  }

  public InfoPanelRankedOption getInfopanelRankedOption() {
    return infopanelRankedOption;
  }

  public void setInfopanelRankedOption(InfoPanelRankedOption infopanelRankedOption) {
    this.infopanelRankedOption = infopanelRankedOption;
  }
}
