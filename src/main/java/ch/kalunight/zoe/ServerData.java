package ch.kalunight.zoe;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ServerStatusRepository;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.constant.Platform;

public class ServerData {

  private static final Logger logger = LoggerFactory.getLogger(ServerData.class);

  private static final List<DTO.Server> serversAskedTreatment = Collections.synchronizedList(new ArrayList<DTO.Server>()); 

  private static final ConcurrentHashMap<String, Boolean> serversIsInTreatment = new ConcurrentHashMap<>();

  private static final Timer serverCheckerThreadTimer = new Timer("ServerChecker-Timer-Executor");
  
  public static final int NBR_PROC = Runtime.getRuntime().availableProcessors();

  private static final ThreadPoolExecutor SERVER_EXECUTOR =
      new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());

  private static final ThreadPoolExecutor INFOCARDS_GENERATOR =
      new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
  
  private static final ThreadPoolExecutor RANKED_MESSAGE_GENERATOR =
      new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());

  private static final Map<Platform, ThreadPoolExecutor> PLAYERS_DATA_EXECUTORS =
      Collections.synchronizedMap(new EnumMap<Platform, ThreadPoolExecutor>(Platform.class));
  
  private static final Map<Platform, ThreadPoolExecutor> MATCH_THREAD_EXECUTORS =
      Collections.synchronizedMap(new EnumMap<Platform, ThreadPoolExecutor>(Platform.class));

  /**
   * Used by event waiter, define in {@link Zoe#main(String[])}
   */
  private static final ScheduledThreadPoolExecutor RESPONSE_WAITER = new ScheduledThreadPoolExecutor(NBR_PROC);

  private ServerData() {
    // Hide public default constructor
  }

  static {
    logger.info("ThreadPools has been lauched with {} threads", NBR_PROC);
    SERVER_EXECUTOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Server-Executor-Thread %d").build());
    INFOCARDS_GENERATOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe InfoCards-Generator-Thread %d").build());
    RANKED_MESSAGE_GENERATOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Ranked-Message-Generator-Thread %d").build());
    RESPONSE_WAITER.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Response-Waiter-Thread %d").build());

    for(Platform platform : Platform.values()) {
      ThreadPoolExecutor executor = new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
      String nameOfThread = String.format("Zoe Match-%s-Worker", platform.getName().toUpperCase());
      executor.setThreadFactory(new ThreadFactoryBuilder().setNameFormat(nameOfThread + " %d").build());
      MATCH_THREAD_EXECUTORS.put(platform, executor);
      
      executor = new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
      nameOfThread = String.format("Zoe Player-Data-%s-Worker", platform.getName().toUpperCase());
      executor.setThreadFactory(new ThreadFactoryBuilder().setNameFormat(nameOfThread + " %d").build());
      PLAYERS_DATA_EXECUTORS.put(platform, executor);
    }
  }

  /**
   * Check if the server will be refreshed or if is actually in treatment.
   * @param server to check
   * @return true if the server is in treatment or if he asked to be treated
   * @throws SQLException 
   */
  public static boolean isServerWillBeTreated(DTO.Server server) throws SQLException {

    boolean serverAskedTreatment = false;
    for(DTO.Server serverWhoAsk : serversAskedTreatment) {
      if(serverWhoAsk.serv_guildId == server.serv_guildId) {
        serverAskedTreatment = true;
      }
    }

    DTO.ServerStatus serverStatus = ServerStatusRepository.getServerStatus(server.serv_guildId);

    return serverAskedTreatment || serverStatus.servstatus_inTreatment;
  }
  
  public static void clearAllTask() {
    RESPONSE_WAITER.getQueue().clear();
    SERVER_EXECUTOR.getQueue().clear();
    INFOCARDS_GENERATOR.getQueue().clear();
    RANKED_MESSAGE_GENERATOR.getQueue().clear();
    
    for(Platform platform : Platform.values()) {
      ThreadPoolExecutor playerWorker = MATCH_THREAD_EXECUTORS.get(platform);
      playerWorker.getQueue().clear();
    }
    
    for(Platform platform : Platform.values()) {
      ThreadPoolExecutor matchWorker = MATCH_THREAD_EXECUTORS.get(platform);
      matchWorker.getQueue().clear();
    }
    logger.info("All queue cleared !");
  }

  public static void shutDownTaskExecutor(TextChannel channel) throws InterruptedException {

    logger.info("Start to shutdown Response Waiter, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Response Waiter, this can take 1 minutes max...").complete();
    RESPONSE_WAITER.shutdown();

    RESPONSE_WAITER.awaitTermination(1, TimeUnit.MINUTES);
    if(!RESPONSE_WAITER.isShutdown()) {
      RESPONSE_WAITER.shutdownNow();
    }
    logger.info("Shutdown of Response Waiter has been completed !");
    channel.sendMessage("Shutdown of Response Waiter has been completed !").complete();

    logger.info("Start to shutdown Servers Executor, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Servers Executor, this can take 1 minutes max...").complete();
    SERVER_EXECUTOR.shutdown();

    SERVER_EXECUTOR.awaitTermination(1, TimeUnit.MINUTES);
    if(!SERVER_EXECUTOR.isShutdown()) {
      SERVER_EXECUTOR.shutdownNow();
    }
    logger.info("Shutdown of Servers Executor has been completed !");
    channel.sendMessage("Shutdown of Servers Executor has been completed !").complete();

    logger.info("Start to shutdown InfoCards Generator, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown InfoCards Generator, this can take 1 minutes max...").complete();
    INFOCARDS_GENERATOR.shutdown();

    INFOCARDS_GENERATOR.awaitTermination(1, TimeUnit.MINUTES);
    if(!INFOCARDS_GENERATOR.isShutdown()) {
      INFOCARDS_GENERATOR.shutdownNow();
    }
    logger.info("Shutdown of InfoCards Generator has been completed !");
    channel.sendMessage("Shutdown of InfoCards Generator has been completed !").complete();
    
    logger.info("Start to shutdown Ranked Message Generator, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Ranked Message Generator, this can take 1 minutes max...").complete();
    RANKED_MESSAGE_GENERATOR.shutdown();

    RANKED_MESSAGE_GENERATOR.awaitTermination(1, TimeUnit.MINUTES);
    if(!RANKED_MESSAGE_GENERATOR.isShutdown()) {
      RANKED_MESSAGE_GENERATOR.shutdownNow();
    }
    logger.info("Shutdown of Ranked Message Generator has been completed !");
    channel.sendMessage("Shutdown of Ranked Message Generator has been completed !").complete();

    logger.info("Start to shutdown Players Data Worker...");
    channel.sendMessage("Start to shutdown Players Data Worker ...").complete();
    for(Platform platform : Platform.values()) {
      ThreadPoolExecutor playerWorker = MATCH_THREAD_EXECUTORS.get(platform);
      playerWorker.shutdown();
      logger.info("Start to shutdown Players Worker {}, this can take 1 minutes max...", platform.getName());
      
      playerWorker.awaitTermination(1, TimeUnit.MINUTES);
      if(!playerWorker.isShutdown()) {
        playerWorker.shutdownNow();
      }
      logger.info("Shutdown of Player Workers {} has been completed !", platform.getName());
    }

    logger.info("Shutdown of Players Data Worker has been completed !");
    channel.sendMessage("Shutdown of Players Data Worker has been completed !").complete();

    channel.sendMessage("Start to shutdown Matchs Worker...").complete();
    for(Platform platform : Platform.values()) {
      ThreadPoolExecutor matchWorker = MATCH_THREAD_EXECUTORS.get(platform);
      matchWorker.shutdown();
      logger.info("Start to shutdown Match Worker {}, this can take 1 minutes max...", platform.getName());

      matchWorker.awaitTermination(5, TimeUnit.MINUTES);
      if(!matchWorker.isShutdown()) {
        matchWorker.shutdownNow();
      }
      logger.info("Shutdown of Match Worker {} has been completed !", platform.getName());
    }

    channel.sendMessage("Shutdown of Matchs Worker has been completed !").complete();
  }
  
  public static int getPlayersDataQueue() {
    int queueTotal = 0;
    for(Platform platform : Platform.values()) {
      queueTotal += MATCH_THREAD_EXECUTORS.get(platform).getQueue().size();
    }
    
    return queueTotal;
  }

  public static ConcurrentMap<String, Boolean> getServersIsInTreatment() {
    return serversIsInTreatment;
  }

  public static List<DTO.Server> getServersAskedTreatment() {
    return serversAskedTreatment;
  }

  public static ThreadPoolExecutor getServerExecutor() {
    return SERVER_EXECUTOR;
  }

  public static ThreadPoolExecutor getInfocardsGenerator() {
    return INFOCARDS_GENERATOR;
  }

  public static ThreadPoolExecutor getPlayersDataWorker(Platform platform) {
    return PLAYERS_DATA_EXECUTORS.get(platform);
  }

  public static ScheduledThreadPoolExecutor getResponseWaiter() {
    return RESPONSE_WAITER;
  }

  public static ThreadPoolExecutor getMatchsWorker(Platform platform) {
    return MATCH_THREAD_EXECUTORS.get(platform);
  }

  public static Timer getServerCheckerThreadTimer() {
    return serverCheckerThreadTimer;
  }

  public static ThreadPoolExecutor getRankedMessageGenerator() {
    return RANKED_MESSAGE_GENERATOR;
  }
}
