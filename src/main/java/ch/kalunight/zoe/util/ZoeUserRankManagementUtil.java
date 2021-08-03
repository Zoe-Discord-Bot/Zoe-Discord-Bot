package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.time.LocalDateTime;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.sub.UserRank;
import ch.kalunight.zoe.repositories.ZoeUserManagementRepository;
import net.dv8tion.jda.api.entities.Member;

public class ZoeUserRankManagementUtil {

  private ZoeUserRankManagementUtil() {
    // hide default public constructor
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
