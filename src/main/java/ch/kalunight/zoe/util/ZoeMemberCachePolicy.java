package ch.kalunight.zoe.util;

import java.util.List;
import ch.kalunight.zoe.repositories.PlayerRepository;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class ZoeMemberCachePolicy implements MemberCachePolicy {
  
  @Override
  public boolean cacheMember(Member member) {
    
    List<Long> registeredPlayersInTheGuild = PlayerRepository.getListDiscordIdOfRegisteredPlayers().get(member.getGuild().getIdLong());
    
    if(registeredPlayersInTheGuild != null) {
      return registeredPlayersInTheGuild.contains(member.getIdLong());
    }
    
    return false;
  }

}
