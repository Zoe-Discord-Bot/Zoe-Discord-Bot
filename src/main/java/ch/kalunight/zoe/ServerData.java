package ch.kalunight.zoe;

import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import ch.kalunight.zoe.model.Server;
import net.dv8tion.jda.core.entities.TextChannel;

public class ServerData {

  private static final Logger logger = LoggerFactory.getLogger(ServerData.class);

  /**
   * Server storage, Key is guild discord id of the concerned server.
   */
  private static final ConcurrentHashMap<String, Server> servers = new ConcurrentHashMap<>();
  
  private static final ConcurrentHashMap<String, Boolean> serversAskedTreatment = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<String, Boolean> serversIsInTreatment = new ConcurrentHashMap<>();

  private static final Timer serverCheckerThreadTimer = new Timer("ServerChecker-Timer-Executor");

  private static int nbProcs = Runtime.getRuntime().availableProcessors();

  private static final ThreadPoolExecutor SERVER_EXECUTOR =
      new ThreadPoolExecutor(nbProcs, nbProcs, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
  
  private static final ThreadPoolExecutor INFOCARDS_GENERATOR =
      new ThreadPoolExecutor(nbProcs, nbProcs, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());

  private ServerData() {
    // Hide public default constructor
  }

  static {
    logger.info("ThreadPools has been lauched with {} threads", nbProcs);
    SERVER_EXECUTOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Server-Executor-Thread %d").build());
    INFOCARDS_GENERATOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe InfoCards-Generator-Thread %d").build());
  }

  public static ConcurrentMap<String, Server> getServers() {
    return servers;
  }

  public static ConcurrentMap<String, Boolean> getServersIsInTreatment() {
    return serversIsInTreatment;
  }
  
  public static ConcurrentMap<String, Boolean> getServersAskedTreatment() {
    return serversAskedTreatment;
  }

  public static ThreadPoolExecutor getServerExecutor() {
    return SERVER_EXECUTOR;
  }

  public static ThreadPoolExecutor getInfocardsGenerator() {
    return INFOCARDS_GENERATOR;
  }
  
  public static Timer getServerCheckerThreadTimer() {
    return serverCheckerThreadTimer;
  }

  public static void shutDownTaskExecutor(TextChannel channel) throws InterruptedException {
    logger.info("Start to shutdown Servers Executor, this can take 5 minutes max...");
    channel.sendMessage("Start to shutdown Servers Executor, this can take 5 minutes max...").complete();
    SERVER_EXECUTOR.shutdown();

    SERVER_EXECUTOR.awaitTermination(5, TimeUnit.MINUTES);
    if(!SERVER_EXECUTOR.isShutdown()) {
      SERVER_EXECUTOR.shutdownNow();
    }
    logger.info("Shutdown of Servers Executor has been completed !");
    channel.sendMessage("Shutdown of Servers Executor has been completed !").complete();
    
    logger.info("Start to shutdown InfoCards Generator, this can take 5 minutes max...");
    channel.sendMessage("Start to shutdown InfoCards Generator, this can take 5 minutes max...").complete();
    INFOCARDS_GENERATOR.shutdown();
    
    INFOCARDS_GENERATOR.awaitTermination(5, TimeUnit.MINUTES);
    if(!INFOCARDS_GENERATOR.isShutdown()) {
      INFOCARDS_GENERATOR.shutdownNow();
    }
    logger.info("Shutdown of InfoCards Generator has been completed !");
    channel.sendMessage("Shutdown of InfoCards Generator has been completed !").complete();
  }
}
