package ch.kalunight.zoe.service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.ServerPerLastRefreshComparator;
import ch.kalunight.zoe.repositories.ServerRepository;

public class TreatServerService {
  
  private static final ServerPerLastRefreshComparator serverOrder = new ServerPerLastRefreshComparator();
  
  private BlockingQueue<Server> serversStatusDetected;
  
  private BlockingQueue<Server> serversAskedToRefresh;
  
  private BlockingQueue<Server> serverToRefreshPassively;
  
  private Set<Server> serverCurrentlyInTreatment;
  
  private ThreadPoolExecutor serverExecutor;
  
  public TreatServerService(ThreadPoolExecutor serverExecutor) {
    serversStatusDetected = new LinkedBlockingQueue<>();
    serversAskedToRefresh = new LinkedBlockingQueue<>();
    serverToRefreshPassively = new LinkedBlockingQueue<>();
    serverCurrentlyInTreatment = Collections.synchronizedSet(new HashSet<DTO.Server>());
    this.serverExecutor = serverExecutor;
  }
  
  public void init() throws SQLException {
    List<Server> allServers = ServerRepository.getAllGuildTreatable();
    
    Collections.sort(allServers, serverOrder);
    
    serverToRefreshPassively.addAll(allServers);
    
    for(int i = 0; i < serverExecutor.getCorePoolSize(); i++) {
      serverToRefreshPassively.poll();
      serverCurrentlyInTreatment.add(e);
    }
  }
  
  public void taskEnded(Server serverTreatmentEnded) {
  }
  
}
