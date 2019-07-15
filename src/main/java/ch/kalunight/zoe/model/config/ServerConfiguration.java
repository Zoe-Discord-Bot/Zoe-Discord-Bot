package ch.kalunight.zoe.model.config;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import ch.kalunight.zoe.model.config.option.ConfigurationOption;
import ch.kalunight.zoe.model.config.option.RegionOption;
import net.dv8tion.jda.core.entities.Role;

public class ServerConfiguration {
  
  /**
   * Define if user can add them self in the system. False if non activated.
   */
  private boolean userSelfAdding = false;
  
  /**
   * Default region of the server. If a account/player is created without the region and this option activated the default region will be used.
   * Null if non activated.
   */
  private RegionOption defaultRegion;
  
  /**
   * This option hide channels of zoe to non-register players. Null if non activated. This id is saved in the save.
   */
  private Role zoeRole;
  
  /**
   * This option activate the command join/leave for everyone. They can join team joinable by everyone.
   */
  private boolean everyoneCanMoveOfTeam = false;

  @Generated("SparkTools")
  private ServerConfiguration(Builder builder) {
    this.userSelfAdding = builder.userSelfAdding;
    this.defaultRegion = builder.defaultRegion;
    this.zoeRole = builder.zoeRole;
    this.everyoneCanMoveOfTeam = builder.everyoneCanMoveOfTeam;
  }

  public ServerConfiguration() {
    this.userSelfAdding = false;
    this.defaultRegion = new RegionOption(null);
    this.zoeRole = null;
    this.everyoneCanMoveOfTeam = false;
  }
  
  public List<ConfigurationOption> getAllConfigurationOption() {
    List<ConfigurationOption> options = new ArrayList<>();
    options.add(defaultRegion);
    return options;
  }

  public boolean isUserSelfAdding() {
    return userSelfAdding;
  }

  public void setUserSelfAdding(boolean userSelfAdding) {
    this.userSelfAdding = userSelfAdding;
  }

  public RegionOption getDefaultRegion() {
    return defaultRegion;
  }

  public void setDefaultRegion(RegionOption defaultRegion) {
    this.defaultRegion = defaultRegion;
  }

  public Role getZoeRole() {
    return zoeRole;
  }

  public void setZoeRole(Role zoeRole) {
    this.zoeRole = zoeRole;
  }

  public boolean isEveryoneCanMoveOfTeam() {
    return everyoneCanMoveOfTeam;
  }

  public void setEveryoneCanMoveOfTeam(boolean everyoneCanMoveOfTeam) {
    this.everyoneCanMoveOfTeam = everyoneCanMoveOfTeam;
  }

  /**
   * Creates builder to build {@link ServerConfiguration}.
   * @return created builder
   */
  @Generated("SparkTools")
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link ServerConfiguration}.
   */
  @Generated("SparkTools")
  public static final class Builder {
    private boolean userSelfAdding;
    private RegionOption defaultRegion;
    private Role zoeRole;
    private boolean everyoneCanMoveOfTeam;

    private Builder() {}

    public Builder withUserSelfAdding(boolean userSelfAdding) {
      this.userSelfAdding = userSelfAdding;
      return this;
    }

    public Builder withDefaultRegion(RegionOption defaultRegion) {
      this.defaultRegion = defaultRegion;
      return this;
    }

    public Builder withZoeRole(Role zoeRole) {
      this.zoeRole = zoeRole;
      return this;
    }

    public Builder withEveryoneCanMoveOfTeam(boolean everyoneCanMoveOfTeam) {
      this.everyoneCanMoveOfTeam = everyoneCanMoveOfTeam;
      return this;
    }

    public ServerConfiguration build() {
      return new ServerConfiguration(this);
    }
  }
}
