package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.sub.UserBenefit;
import ch.kalunight.zoe.model.sub.UserRank;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.ZoeUserManagementRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

public class ZoeUserRankManagementUtil {

  private static final Logger logger = LoggerFactory.getLogger(ZoeUserRankManagementUtil.class);
  
  private static List<DTO.Role> allRoles;
  
  private ZoeUserRankManagementUtil() {
    // hide default public constructor
  }
  
  public static boolean isServerHaveAccessToEarlyAccessFeature(Guild guild) throws SQLException {
    List<DTO.Player> players = PlayerRepository.getPlayers(guild.getIdLong());
    
    List<Member> memberWithAdminPermissions = new ArrayList<>();
    for(DTO.Player player : players) {
      Member member = guild.getMemberById(player.player_discordId);
      
      if(member != null && member.hasPermission(Permission.ADMINISTRATOR)) {
        memberWithAdminPermissions.add(member);
      }
    }
    
    for(Member admin : memberWithAdminPermissions) {
      List<DTO.ZoeUserRole> subs = ZoeUserManagementRepository.getZoeSubscriptionByDiscordId(admin.getIdLong());
      List<DTO.Role> roles = getRolesByAssignedRoles(subs);
      
      for(DTO.Role role : roles) {
        UserRank rank = UserRank.getUserRankByRoleId(role.role_roleId);
        
        List<UserBenefit> benefits = UserBenefit.getBenefitsByUserRank(rank);
        
        if(benefits.contains(UserBenefit.ACCESS_NEW_FEATURES_HUGE_SERVERS)) {
          return true;
        }
        
        if(benefits.contains(UserBenefit.ACCESS_NEW_FEATURES_SMALL_AND_MEDIUM_SERVERS) 
            && UserBenefit.HUGE_SERVERS_START >= guild.getMemberCount()) {
          return true;
        }
      }
    }
    return false;
  }
  
  public static String getEmotesByDiscordId(long discordId) {
    try {
    List<DTO.ZoeUserRole> assignedRoles = ZoeUserManagementRepository.getZoeSubscriptionByDiscordId(discordId);
    if(assignedRoles.isEmpty()) {
      return "";
    }
    
    List<DTO.Role> roles = getRolesByAssignedRoles(assignedRoles);
    
    StringBuilder badges = new StringBuilder();
    
    boolean alreadyABadge = false;
    UserRank dev = findUserRankInsideRoles(roles, UserRank.DEV);
    if(dev != null) {
      badges.append(dev.getEmoteWithUser(null));
      alreadyABadge = true;
    }
    
    UserRank staff = findUserRankInsideRoles(roles, UserRank.STAFF);
    if(staff != null) {
      badges.append(staff.getEmoteWithUser(null));
      alreadyABadge = true;
    }
    
    UserRank exceptionnalFeaturesAccess = findUserRankInsideRoles(roles, UserRank.EXCEPTIONNAL_FEATURES_ACCESS);
    if(exceptionnalFeaturesAccess != null) {
      badges.append(exceptionnalFeaturesAccess.getEmoteWithUser(null));
      alreadyABadge = true;
    }
    
    UserRank sub = findUserRankInsideRoles(roles, UserRank.SUB_TIER_3);
    if(sub == null) {
      sub = findUserRankInsideRoles(roles, UserRank.SUB_TIER_2);
    }
    
    if(sub == null) {
      sub = findUserRankInsideRoles(roles, UserRank.SUB_TIER_1);
    }
    
    if(sub != null) {
      DTO.ZoeUser user = ZoeUserManagementRepository.getZoeUserByDiscordId(discordId);
      badges.append(sub.getEmoteWithUser(user));
      alreadyABadge = true;
    }
    
    if(alreadyABadge) {
      badges.append(" ");
    }
    
    return badges.toString();
    }catch (SQLException e) {
      logger.warn("SQL Error while getting player badge!", e);
      return "";
    }
  }
  
  private static UserRank findUserRankInsideRoles(List<DTO.Role> roles, UserRank rank) {
    for(DTO.Role role : roles) {
      if(rank.getId() == role.role_roleId) {
        return rank;
      }
    }
    return null;
  }
  
  public static List<DTO.Role> getRolesByAssignedRoles(List<DTO.ZoeUserRole> assignedRoles) throws SQLException{
    List<DTO.Role> roles = getAllZoeRoles();
    List<DTO.Role> correspondingRoles = new ArrayList<>();
    
    for(DTO.ZoeUserRole assignedRole : assignedRoles) {
      for(DTO.Role possibleRole : roles) {
        if(possibleRole.role_id == assignedRole.zoeUserRole_fk_role_id) {
          correspondingRoles.add(possibleRole);
          break;
        }
      }
    }
    
    return correspondingRoles;
  }
  
  private static List<DTO.Role> getAllZoeRoles() throws SQLException {
    if(allRoles == null) {
      allRoles = ZoeUserManagementRepository.getAllZoeRole();
    }
    return allRoles;
  }

  public static void addUserRank(UserRank rank, Member member) throws SQLException {
    DTO.ZoeUser user = ZoeUserManagementRepository.getZoeUserByDiscordId(member.getIdLong());
    
    if(user == null) {
      ZoeUserManagementRepository.createZoeUser(member.getIdLong());
      user = ZoeUserManagementRepository.getZoeUserByDiscordId(member.getIdLong());
    }
    
    DTO.ZoeUserRole zoeUserSubscription = ZoeUserManagementRepository.getZoeSubscriptionByDiscordIdAndRole(
        member.getIdLong(), rank.getId());
    
    if(zoeUserSubscription == null) {
      DTO.Role role = ZoeUserManagementRepository.getZoeRoleByRoleId(rank.getId());
      
      LocalDateTime endOfSub = null;
      if(rank.isSupporter()) {
        endOfSub = LocalDateTime.now().plusMonths(1);
        ZoeUserManagementRepository.updateZoeUser(user.zoeUser_discordId,
            user.zoeUser_fullMonthSupported, user.zoeUser_totalGiven + rank.getValue());
      }
      
      ZoeUserManagementRepository.createZoeSubscription(user.zoeUser_id, role.role_id, endOfSub);
    }
  }

  public static void removeUserRank(UserRank rankLost, Long userId) throws SQLException {
    DTO.ZoeUser user = ZoeUserManagementRepository.getZoeUserByDiscordId(userId);
    DTO.Role role = ZoeUserManagementRepository.getZoeRoleByRoleId(rankLost.getId());
    
    if(user == null) {
      return;
    }
    
    ZoeUserManagementRepository.deleteZoeSubscription(user.zoeUser_id, role.role_id);
  }
}
