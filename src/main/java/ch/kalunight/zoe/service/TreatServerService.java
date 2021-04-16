package ch.kalunight.zoe.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DatedServer;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.ServerPerLastRefreshComparator;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.service.infochannel.InfoPanelRefresher;

public class TreatServerService {

  private static final ServerPerLastRefreshComparator serverOrder = new ServerPerLastRefreshComparator();

  private static final Logger logger = LoggerFactory.getLogger(TreatServerService.class);

  private BlockingQueue<Server> serversStatusDetected;

  private BlockingQueue<Server> serversAskedToRefresh;

  private BlockingQueue<Server> serverToRefreshPassively;

  private Set<Server> serverCurrentlyInTreatment;

  private LocalDateTime cycleStart;

  private ThreadPoolExecutor serverExecutor;

  private List<DatedServer> lastServerRefreshed;

  private int numberOfServerManaged;

  private int serverTreatedPerMin;

  private int nbrOfMinutesToRefreshAllServers;

  private LocalDateTime serverTreatedLastRefresh;

  private LocalDateTime nbrOfMinutesToRefreshAllServersLastRefresh;

  public TreatServerService(ThreadPoolExecutor serverExecutor) {
    serversStatusDetected = new LinkedBlockingQueue<>();
    serversAskedToRefresh = new LinkedBlockingQueue<>();
    serverToRefreshPassively = new LinkedBlockingQueue<>();
    serverCurrentlyInTreatment = Collections.synchronizedSet(new HashSet<DTO.Server>());
    lastServerRefreshed = Collections.synchronizedList(new ArrayList<DatedServer>());
    this.serverExecutor = serverExecutor;
    init();
  }

  private synchronized void init() {
    logger.info("Init a new cycle of servers refresh");
    List<Server> allServers = null;

    do {
      try {
        allServers = ServerRepository.getAllGuildTreatable();
        numberOfServerManaged = allServers.size();
      } catch (SQLException e) {
        logger.error("Error while getting treatable guild. Retrying in 3 secs ...", e);
        try {
          TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e1) {
          logger.error("Thread interrupted !", e1);
          Thread.currentThread().interrupt();
        }
      }
    } while(allServers == null); 

    Collections.sort(allServers, serverOrder);

    serverToRefreshPassively.addAll(allServers);

    cycleStart = LocalDateTime.now();

    logger.info("Start the process with {} threads. (Less if a small size amount of servers is detected)", serverExecutor.getCorePoolSize());

    for(int i = 0; i < serverExecutor.getCorePoolSize(); i++) {
      Server server = serverToRefreshPassively.poll();
      if(server != null) {
        logger.info("Refresh Server Thread {} Started !", i);
        serverCurrentlyInTreatment.add(server);
        serverExecutor.execute(new InfoPanelRefresher(server, false));
      }
    }
  }

  public synchronized void taskEnded(Server serverTreatmentEnded) {

    if(serverTreatmentEnded != null) {
      serverCurrentlyInTreatment.remove(serverTreatmentEnded);

      lastServerRefreshed.add(new DatedServer(serverTreatmentEnded, LocalDateTime.now()));
    }

    synchronized (serversStatusDetected) {
      if(!serversStatusDetected.isEmpty()) {
        Server server = serversStatusDetected.poll();
        serverToRefreshPassively.remove(server);

        if(!serverCurrentlyInTreatment.contains(server)) {
          serverCurrentlyInTreatment.add(server);
          serverExecutor.execute(new InfoPanelRefresher(server, false));
        }else {
          taskEnded(null);
        }
        return;
      } 
    }

    synchronized (serversAskedToRefresh) {
      if (!serversAskedToRefresh.isEmpty()) {
        Server server = serversAskedToRefresh.poll();
        serverToRefreshPassively.remove(server);

        if(!serverCurrentlyInTreatment.contains(server)) {
          serverCurrentlyInTreatment.add(server);
          serverExecutor.execute(new InfoPanelRefresher(server, true));
        }else {
          taskEnded(null);
        }
        return;
      } 
    }

    synchronized (serverToRefreshPassively) {
      if(!serverToRefreshPassively.isEmpty()) {
        Server server = serverToRefreshPassively.poll();
        server = refreshServer(server);

        if(!serverCurrentlyInTreatment.contains(server)) {
          serverCurrentlyInTreatment.add(server);
          serverExecutor.execute(new InfoPanelRefresher(server, true));
        }else {
          taskEnded(null);
        }
        return;
      }

      logger.info("All servers Refresh Passive Task Done !");
      if(!cycleStart.isBefore(LocalDateTime.now().minusMinutes(1))) {
        long secondsToWait = LocalDateTime.now().until(cycleStart.plusMinutes(1), ChronoUnit.SECONDS);
        logger.info("Refresh to fast detected ! Wait {} seconds before restarting the process", secondsToWait);
        if(secondsToWait > 0) {
          try {
            TimeUnit.SECONDS.sleep(secondsToWait);
          } catch (InterruptedException e) {
            logger.error("Thread inturrupted !", e);
            Thread.currentThread().interrupt();
          }
        }
      }
      reloadPassiveRefreshQueue();
      Server server = serverToRefreshPassively.poll();

      serverCurrentlyInTreatment.add(server);
      serverExecutor.execute(new InfoPanelRefresher(server, false));
    }
  }

  public synchronized void reloadPassiveRefreshQueue() {
    List<Server> allServers = null;

    do {
      try {
        allServers = ServerRepository.getAllGuildTreatable();
        numberOfServerManaged = allServers.size();
      } catch (SQLException e) {
        logger.error("Error while getting treatable guild. Retrying in 3 secs ...", e);
        try {
          TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e1) {
          logger.error("Thread interrupted !", e1);
          Thread.currentThread().interrupt();
        }
      }
    } while(allServers == null); 

    Collections.sort(allServers, serverOrder);

    serverToRefreshPassively.addAll(allServers);

    logger.info("Server Refresh Passive Queue feeded with {} servers", numberOfServerManaged);

    cycleStart = LocalDateTime.now();
  }

  public synchronized int getServerRefreshedEachMinute() {

    if(serverTreatedLastRefresh == null || serverTreatedLastRefresh.isBefore(LocalDateTime.now().minusMinutes(3))) {
      List<DatedServer> serversToRemove = new ArrayList<>();

      for(DatedServer serverToCheck : lastServerRefreshed) {
        if(serverToCheck.getLocalDateTime().isBefore(LocalDateTime.now().minusMinutes(1))) {
          serversToRemove.add(serverToCheck);
        }
      }

      lastServerRefreshed.removeAll(serversToRemove);

      serverTreatedLastRefresh = LocalDateTime.now();
      serverTreatedPerMin = lastServerRefreshed.size();

      return lastServerRefreshed.size();
    }else {
      return serverTreatedPerMin;
    }
  }

  public synchronized int getEstimateTimeToFullRefreshInMinutes() {

    if(nbrOfMinutesToRefreshAllServersLastRefresh == null || nbrOfMinutesToRefreshAllServersLastRefresh.isBefore(LocalDateTime.now().minusMinutes(3))) {
      int serverRefreshEachMinutes = getServerRefreshedEachMinute();

      if(serverRefreshEachMinutes <= 0) {
        serverRefreshEachMinutes = 1;
      }

      double nbrOfMinutesToRefreshAllServersDouble = numberOfServerManaged / (double) serverRefreshEachMinutes;

      nbrOfMinutesToRefreshAllServersLastRefresh = LocalDateTime.now();

      nbrOfMinutesToRefreshAllServers = (int) nbrOfMinutesToRefreshAllServersDouble;
    }

    return nbrOfMinutesToRefreshAllServers;
  }

  private Server refreshServer(Server server) {
    try {
      return ServerRepository.getServerWithServId(server.serv_id);
    } catch (SQLException e) {
      logger.error("Error while refreshing server !", e);
    }
    return server;
  }

  public BlockingQueue<Server> getServersStatusDetected() {
    return serversStatusDetected;
  }

  public BlockingQueue<Server> getServersAskedToRefresh() {
    return serversAskedToRefresh;
  }

  public BlockingQueue<Server> getServerToRefreshPassively() {
    return serverToRefreshPassively;
  }

  public int getNumberOfServerManaged() {
    return numberOfServerManaged;
  }

  public int getQueueSizeDiscordStatus() {
    return serversStatusDetected.size();
  }

  public int getQueueSizeAskedToRefresh() {
    return serversAskedToRefresh.size();
  }

  public int getQueueSizePassiveRefresh() {
    return serverToRefreshPassively.size();
  }

  public LocalDateTime getCycleStart() {
    return cycleStart;
  }

}
