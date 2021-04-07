package ch.kalunight.zoe.model.dto;

import java.time.LocalDateTime;
import java.util.Comparator;

import ch.kalunight.zoe.model.dto.DTO.Server;

/**
 * Order by the oldest local time is first
 */
public class ServerPerLastRefreshComparator implements Comparator<DTO.Server> {

  @Override
  public int compare(Server o1, Server o2) {
    
    LocalDateTime firstRefresh = o1.serv_lastRefresh;
    
    LocalDateTime secondRefresh = o2.serv_lastRefresh;
    
    if(firstRefresh.isEqual(secondRefresh)) {
      return 0;
    }
    
    if(firstRefresh.isBefore(secondRefresh)) {
      return -1;
    }
    
    if(firstRefresh.isAfter(secondRefresh)) {
      return 1;
    }
    
    return 0;
  }

}
