package ch.kalunight.zoe.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ZoeSubscriptionListener;
import ch.kalunight.zoe.repositories.PlayerRepository;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class ZoeMemberCachePolicy implements MemberCachePolicy {

  public static final Logger logger = LoggerFactory.getLogger(ZoeMemberCachePolicy.class);

  @Override
  public boolean cacheMember(Member member) {
    if(member.getGuild().getId().equals(ZoeSubscriptionListener.SUBSCRIPTION_SERVER_ID)) {
      return true;
    }
    
    try {
      List<Long> registeredPlayersInTheGuild = PlayerRepository.getListDiscordIdOfRegisteredPlayers().get(member.getGuild().getIdLong());
      if(registeredPlayersInTheGuild != null) {
        return registeredPlayersInTheGuild.contains(member.getIdLong());
      }
    }catch(Exception e) {
      logger.error("Error while doing the choice of caching or not ! The member will not be cached ! Exception :", e);
    }
    return false;
  }

}
