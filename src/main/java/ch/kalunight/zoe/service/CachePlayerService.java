package ch.kalunight.zoe.service;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.util.Ressources;

public class CachePlayerService implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(CachePlayerService.class);

  @Override
  public void run() {
    try {
      Set<Entry<Long, List<Long>>> listEntry = PlayerRepository.getListDiscordIdOfRegisteredPlayers().entrySet();

      int count = 0;
      for(Entry<Long, List<Long>> oneGuild : listEntry) {
        count++;
        if(!Ressources.getBlackListedServer().contains(oneGuild.getKey())) {
          logger.info("Load server guild {}/{}", count, listEntry.size());
          for(Long idPlayer : oneGuild.getValue()) {
            if(oneGuild != null && oneGuild.getKey() != null && Zoe.getJda().getGuildById(oneGuild.getKey()) != null) {
              Zoe.getJda().getGuildById(oneGuild.getKey()).retrieveMemberById(idPlayer).complete();
            }
          }
        }
      }

      logger.info("Load of all guild Ended !");

    } catch (Exception e) {
      logger.error("Error while loading all players !", e);
    }
  }
}
