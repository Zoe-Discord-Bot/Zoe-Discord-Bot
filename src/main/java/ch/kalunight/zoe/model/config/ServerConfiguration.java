package ch.kalunight.zoe.model.config;

import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.model.config.option.ConfigurationOption;
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
   * This option activate the command join/leave for everyone. They can join team joinable by everyone.
   */
  private boolean everyoneCanMoveOfTeam = false;

  public List<ConfigurationOption> getAllConfigurationOption() {
    List<ConfigurationOption> options = new ArrayList<>();
    options.add(defaultRegion);
    options.add(zoeRoleOption);
    options.add(userSelfAdding);
    return options;
  }
  
  public ServerConfiguration() {
    this.defaultRegion = new RegionOption();
    this.zoeRoleOption = new RoleOption();
    this.userSelfAdding = new SelfAddingOption();
    this.everyoneCanMoveOfTeam = false;
  }

  public RegionOption getDefaultRegion() {
    return defaultRegion;
  }

  public void setDefaultRegion(RegionOption defaultRegion) {
    this.defaultRegion = defaultRegion;
  }

  public RoleOption getZoeRoleOption() {
    return zoeRoleOption;
  }

  public void setZoeRoleOption(RoleOption zoeRole) {
    this.zoeRoleOption = zoeRole;
  }

  public boolean isEveryoneCanMoveOfTeam() {
    return everyoneCanMoveOfTeam;
  }

  public void setEveryoneCanMoveOfTeam(boolean everyoneCanMoveOfTeam) {
    this.everyoneCanMoveOfTeam = everyoneCanMoveOfTeam;
  }

  public SelfAddingOption getUserSelfAdding() {
    return userSelfAdding;
  }

  public void setUserSelfAdding(SelfAddingOption userSelfAdding) {
    this.userSelfAdding = userSelfAdding;
  }
}
