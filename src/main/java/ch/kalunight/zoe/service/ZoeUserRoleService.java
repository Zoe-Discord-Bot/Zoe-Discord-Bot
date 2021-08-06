package ch.kalunight.zoe.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.ZoeSubscriptionListener;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.sub.UserRank;
import ch.kalunight.zoe.repositories.ZoeUserManagementRepository;
import ch.kalunight.zoe.util.ZoeUserRankManagementUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class ZoeUserRoleService implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ZoeUserRoleService.class);

  @Override
  public void run() {
    try {
      Guild guild = Zoe.getGuildById(ZoeSubscriptionListener.SUBSCRIPTION_SERVER_ID);

      if(guild == null) {
        return;
      }

      updateDbRoleAccordingToServer(guild);
      updateCurrentSubscriptionMonth();
      
    }catch (SQLException e) {
      logger.error("SQL Exception during the refresh of Zoe roles", e);
    }catch (Exception e) {
      logger.error("Unexpected exception during the refresh of Zoe roles", e);
    }
  }

  private void updateCurrentSubscriptionMonth() throws SQLException {
    for(UserRank rankToUpdate : UserRank.values()) {

      if(!rankToUpdate.isSupporter()) {
        continue;
      }
      
      List<DTO.ZoeUserRole> allSubscriptions = ZoeUserManagementRepository.getZoeSubscriptionByRole(rankToUpdate.getId());
      
      for(DTO.ZoeUserRole subscriptionToCheck : allSubscriptions) {
        if(subscriptionToCheck.zoeUserRole_endOfTheSubscription.isBefore(LocalDateTime.now())) {
          DTO.ZoeUser userToUpdate = ZoeUserManagementRepository.getZoeUserById(subscriptionToCheck.zoeUserRole_fk_user_id);
          
          ZoeUserManagementRepository.updateZoeUser(userToUpdate.zoeUser_discordId,
              userToUpdate.zoeUser_fullMonthSupported + 1, userToUpdate.zoeUser_totalGiven + rankToUpdate.getValue());
          
          ZoeUserManagementRepository.updateZoeSubscription(subscriptionToCheck.zoeUserRole_fk_user_id,
              subscriptionToCheck.zoeUserRole_fk_role_id, subscriptionToCheck.zoeUserRole_endOfTheSubscription.plusMonths(1));
        }
      }
    }
  }

  private void updateDbRoleAccordingToServer(Guild guild) throws SQLException {
    for(UserRank rankToCheck : UserRank.values()) {
      Role role = guild.getRoleById(rankToCheck.getRoleId());

      if(role == null) {
        logger.warn("Role id for {} badly defined! Role not found in the guild.", rankToCheck.toString());
        continue;
      }

      List<Member> membersWithRoles = guild.getMembersWithRoles(role);
      List<DTO.ZoeUser> usersWithTheRole = ZoeUserManagementRepository.getZoeUsersByRoleId(rankToCheck.getId());
      for(Member memberWithTheRole : membersWithRoles) {
        boolean userHasTheRole = false;
        DTO.ZoeUser userInDb = null;
        
        for(DTO.ZoeUser userToCheck : usersWithTheRole) {
          if(memberWithTheRole.getIdLong() == userToCheck.zoeUser_discordId) {
            userHasTheRole = true;
            userInDb = userToCheck;
            break;
          }
        }
        
        if(userHasTheRole) {
          usersWithTheRole.remove(userInDb);
        }else {
          ZoeUserRankManagementUtil.addUserRank(rankToCheck, memberWithTheRole);
        }
      }
      
      /*
       * if usersWithTheRole is not empty,
       * this mean these user have the role in db but not in server.
       * So we delete it in db.
       */
      for(DTO.ZoeUser userToDelete : usersWithTheRole) {
        ZoeUserRankManagementUtil.removeUserRank(rankToCheck, userToDelete.zoeUser_discordId);
      }
    }
  }

}
