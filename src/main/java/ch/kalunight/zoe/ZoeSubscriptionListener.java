package ch.kalunight.zoe;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.sub.UserRank;
import ch.kalunight.zoe.util.ZoeUserRankManagementUtil;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ZoeSubscriptionListener extends ListenerAdapter {

  public static final String SUBSCRIPTION_SERVER_ID = "554578876811182082";

  private static final Logger logger = LoggerFactory.getLogger(ZoeSubscriptionListener.class);

  @Override
  public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
    Runnable task = getOnGuildMemberRoleAddRunnable(event);
    ServerThreadsManager.getEventsExecutor().execute(task);
  }

  private Runnable getOnGuildMemberRoleAddRunnable(GuildMemberRoleAddEvent event) {
    return new Runnable() {

      @Override
      public void run() {

        for(Role roleGiven : event.getRoles()) {
          UserRank rankReceived = UserRank.getUserRankByRoleId(roleGiven.getId());

          if(rankReceived != null) {
            try {
              ZoeUserRankManagementUtil.addUserRank(rankReceived, event.getMember());
            } catch (SQLException e) {
              logger.error("Error while adding role to user in db side!", e);
            }
          }
        }
      }
    };
  }

  @Override
  public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
    Runnable task = getOnGuildMemberRoleRemoveRunnable(event);
    ServerThreadsManager.getEventsExecutor().execute(task);
  }

  private Runnable getOnGuildMemberRoleRemoveRunnable(GuildMemberRoleRemoveEvent event) {
    return new Runnable() {

      @Override
      public void run() {
        for(Role roleLost : event.getRoles()) {
          UserRank rankLost = UserRank.getUserRankByRoleId(roleLost.getId());

          if(rankLost != null) {
            try {
              ZoeUserRankManagementUtil.removeUserRank(rankLost, event.getMember().getIdLong());
            } catch (SQLException e) {
              logger.error("Error while removing role to user in db side!", e);
            }
          }
        }
      }
    };
  }


}