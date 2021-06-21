package ch.kalunight.zoe.service.infochannel;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.service.ServerChecker;

public class DelayedInfochannelRefresh extends TimerTask {

  private static final Map<Long, AtomicInteger> gameChangeDetectedByServer = Collections.synchronizedMap(new HashedMap<Long, AtomicInteger>());

  private static final Logger logger = LoggerFactory.getLogger(DelayedInfochannelRefresh.class);

  private Server server;

  private int knownDelayedTime;

  public DelayedInfochannelRefresh(Server server, int knownDelayedTime) {
    this.server = server;
    this.knownDelayedTime = knownDelayedTime;
  }

  @Override
  public void run() {
    try {
      boolean needToBeTreated = false;

      synchronized (gameChangeDetectedByServer) {
        AtomicInteger askedRefresh = gameChangeDetectedByServer.get(server.serv_guildId);
        if(askedRefresh.get() == knownDelayedTime) {
          askedRefresh.set(0);

          needToBeTreated = true;
        }
      }

      if(needToBeTreated) {
        ServerThreadsManager.getServersIsInTreatment().put(Long.toString(server.serv_guildId), true);
        ServerRepository.updateTimeStamp(server.serv_guildId, LocalDateTime.now());
        if(ServerChecker.getServerRefreshService() != null) {
          ServerChecker.getServerRefreshService().getServersStatusDetected().add(server);
        }
      }
    }catch (SQLException e) {
      logger.error("SQL Expception in delayed Infochannel Refresh", e);
    }catch (Exception e) {
      logger.error("Unexpected error in delayed Infochannel Refresh", e);
    }
  }

  public static Map<Long, AtomicInteger> getGameChangeDetectedByServer() {
    return gameChangeDetectedByServer;
  }

}
