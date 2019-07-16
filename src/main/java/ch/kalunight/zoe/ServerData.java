package ch.kalunight.zoe;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.model.Server;

public class ServerData {

  private static final Logger logger = LoggerFactory.getLogger(ServerData.class);

  /**
   * Server storage, Key is guild discord id of the concerned server.
   */
  private static final ConcurrentHashMap<String, Server> servers = new ConcurrentHashMap<>();
  
  private static final ConcurrentHashMap<String, Boolean> serversAskedTreatment = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<String, Boolean> serversIsInTreatment = new ConcurrentHashMap<>();

  private static final Timer mainThreadTimer = new Timer();

  private static int nbProcs = Runtime.getRuntime().availableProcessors();

  private static final ThreadPoolExecutor TASK_EXECUTOR =
      new ThreadPoolExecutor(nbProcs, nbProcs, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());

  private ServerData() {
    // Hide public default constructor
  }

  static {
    logger.info("Task executor lauched with {} threads", nbProcs);
  }

  public static Map<String, Server> getServers() {
    return servers;
  }

  public static Map<String, Boolean> getServersIsInTreatment() {
    return serversIsInTreatment;
  }

  public static ThreadPoolExecutor getTaskExecutor() {
    return TASK_EXECUTOR;
  }

  public static Timer getMainThreadTimer() {
    return mainThreadTimer;
  }

  public static void shutDownTaskExecutor() throws InterruptedException {
    TASK_EXECUTOR.shutdown();

    TASK_EXECUTOR.awaitTermination(10, TimeUnit.MINUTES);
  }

  public static ConcurrentHashMap<String, Boolean> getServersAskedTreatment() {
    return serversAskedTreatment;
  }
}
