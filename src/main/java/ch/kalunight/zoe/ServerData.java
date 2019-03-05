package ch.kalunight.zoe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import ch.kalunight.zoe.model.Server;

public class ServerData {

  private ServerData() {}
  
  private static final ConcurrentHashMap<String, Server> servers = new ConcurrentHashMap<>();
  
  private static int nbProcs = Runtime.getRuntime().availableProcessors();
  
  private static final ThreadPoolExecutor TASK_EXECUTOR =
      new ThreadPoolExecutor(1, nbProcs, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

  public static Map<String, Server> getServers() {
    return servers;
  }

  public static ThreadPoolExecutor getTaskExecutor() {
    return TASK_EXECUTOR;
  }
  
  public static void shutDownTaskExecutor() throws InterruptedException {
    TASK_EXECUTOR.shutdown();
    
    TASK_EXECUTOR.awaitTermination(10, TimeUnit.MINUTES);
  }
}
