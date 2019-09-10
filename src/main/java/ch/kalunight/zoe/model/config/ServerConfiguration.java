package ch.kalunight.zoe.model.config;

import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.model.config.option.ConfigurationOption;
import ch.kalunight.zoe.model.config.option.InfoCardOption;
import ch.kalunight.zoe.model.config.option.RegionOption;
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
  private InfoCardOption infoCardsOption;
  
  /**
   * This option activate the command join/leave for everyone. They can join team joinable by everyone. NOT IMPLEMENTED
   */
  private boolean everyoneCanMoveOfTeam = false;

  public List<ConfigurationOption> getAllConfigurationOption() {
    List<ConfigurationOption> options = new ArrayList<>();
    options.add(defaultRegion);
    options.add(zoeRoleOption);
    options.add(userSelfAdding);
    options.add(infoCardsOption);
    return options;
  }
  
  public ServerConfiguration() {
    this.defaultRegion = new RegionOption();
    this.zoeRoleOption = new RoleOption();
    this.userSelfAdding = new SelfAddingOption();
    this.infoCardsOption = new InfoCardOption();
    this.everyoneCanMoveOfTeam = false;
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

  public InfoCardOption getInfoCardsOption() {
    return infoCardsOption;
  }
}
