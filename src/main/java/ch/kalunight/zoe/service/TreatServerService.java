package ch.kalunight.zoe.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
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

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DatedServer;
import ch.kalunight.zoe.model.dto.ServerPerLastRefreshComparator;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.service.infochannel.InfoCardsWorker;
import ch.kalunight.zoe.service.infochannel.InfoPanelRefresher;

public class TreatServerService {

  private static final Logger logger = LoggerFactory.getLogger(TreatServerService.class);

  private static final ServerPerLastRefreshComparator serverOrder = new ServerPerLastRefreshComparator();

  private static final boolean IN_TEST = true;

  private BlockingQueue<Server> serversStatusDetected;

  private BlockingQueue<Server> serversAskedToRefresh;

  private BlockingQueue<InfoCardsWorker> infocardsToRefresh;

  private BlockingQueue<Server> serverToRefreshPassively;

  private Set<Server> serverCurrentlyInTreatment;

  private ThreadPoolExecutor serverExecutor;

  private List<DatedServer> lastServerRefreshed;

  private int numberOfServerManaged;

  private int serverTreatedPerMin;

  private int nbrOfMinutesToRefreshAllServers;
  
  private LocalDateTime cycleStart;

  private LocalDateTime serverTreatedLastRefresh;

  private LocalDateTime nbrOfMinutesToRefreshAllServersLastRefresh;

  public TreatServerService(ThreadPoolExecutor serverExecutor) {
    serversStatusDetected = new LinkedBlockingQueue<>();
    serversAskedToRefresh = new LinkedBlockingQueue<>();
    infocardsToRefresh = new LinkedBlockingQueue<>();
    serverCurrentlyInTreatment = Collections.synchronizedSet(new HashSet<DTO.Server>());
    serverToRefreshPassively = new LinkedBlockingQueue<>();
    lastServerRefreshed = Collections.synchronizedList(new ArrayList<DatedServer>());
    this.serverExecutor = serverExecutor;
    cycleStart = LocalDateTime.now();
    init();
  }

  private synchronized void init() {
    logger.info("Start the process with {} threads.", serverExecutor.getCorePoolSize());

    for(int i = 0; i < serverExecutor.getCorePoolSize(); i++) {
      serverExecutor.execute(new WaitTaskRefresh(this));
    }
  }

  public synchronized void taskEnded(Server serverTreatmentEnded) {

    if(serverTreatmentEnded != null) {
      serverCurrentlyInTreatment.remove(serverTreatmentEnded);

      lastServerRefreshed.add(new DatedServer(serverTreatmentEnded, LocalDateTime.now()));
    }

    if(Zoe.isShutdownStarted()) {
      return;
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

    synchronized (infocardsToRefresh) {
      if (!infocardsToRefresh.isEmpty()) {
        InfoCardsWorker infoCardWorker = infocardsToRefresh.poll();

        serverExecutor.execute(infoCardWorker);
        return;
      }
    }

    synchronized (serverToRefreshPassively) {
      if(!serverToRefreshPassively.isEmpty()) {
        Server server = serverToRefreshPassively.poll();
        server = refreshServer(server);

        if(!serverCurrentlyInTreatment.contains(server) && !IN_TEST) {
          serverCurrentlyInTreatment.add(server);
          serverExecutor.execute(new InfoPanelRefresher(server, true));
        }else {
          taskEnded(null);
        }
        return;
      } else {
        refillPassiveQueue();
      }
    }


    try {
      TimeUnit.MILLISECONDS.sleep(100);
      serverExecutor.execute(new WaitTaskRefresh(this));
    } catch (InterruptedException e) {
      logger.error("InterruptedException while waiting for a task.");
      serverExecutor.execute(new WaitTaskRefresh(this));
      Thread.currentThread().interrupt();
    }
  }

  private void refillPassiveQueue() {
    
    List<Server> allServers;
    try {
      allServers = ServerRepository.getAllGuildTreatable();
      cycleStart = LocalDateTime.now();
    } catch (SQLException e) {
      logger.warn("Error while getting all servers which need to be refreshed", e);
      return;
    }
    numberOfServerManaged = allServers.size();

    Collections.sort(allServers, serverOrder);

    serverToRefreshPassively.addAll(allServers);
  }

  public int getServerRefreshedEachMinute() {

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

  public int getEstimateTimeToFullRefreshInMinutes() {

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

  public BlockingQueue<InfoCardsWorker> getInfocardsToRefresh() {
    return infocardsToRefresh;
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

  public int getQueueSizeInfoCardsToRefresh() {
    return infocardsToRefresh.size();
  }

  public int getQueueSizePassiveRefresh() {
    return serverToRefreshPassively.size();
  }
  
  public LocalDateTime getCycleStart() {
    return cycleStart;
  }

}
