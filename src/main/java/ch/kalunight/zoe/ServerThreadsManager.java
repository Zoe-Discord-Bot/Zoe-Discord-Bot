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
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.model.dto.ZoeRegion;
import ch.kalunight.zoe.repositories.ServerStatusRepository;
import net.dv8tion.jda.api.entities.TextChannel;

public class ServerThreadsManager {

  private static final Logger logger = LoggerFactory.getLogger(ServerThreadsManager.class);

  private static final List<DTO.Server> serversAskedTreatment = Collections.synchronizedList(new ArrayList<DTO.Server>()); 

  private static final ConcurrentHashMap<String, Boolean> serversIsInTreatment = new ConcurrentHashMap<>();

  private static final Timer serverCheckerThreadTimer = new Timer("ServerChecker-Timer-Executor");
    
  private static final Timer DISCORD_DETECTION_DELAYED_TASK = new Timer("Zoe Discord-Status-Delayed-Refresh-Timer");
  
  public static final int NBR_PROC = Runtime.getRuntime().availableProcessors();

  private static final ThreadPoolExecutor DATA_ANALYSIS_MANAGER =
      new ThreadPoolExecutor(1, 1, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
  
  private static final ThreadPoolExecutor DATA_ANALYSIS_THREAD =
      new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
  
  private static final ThreadPoolExecutor MONITORING_DATA_EXECUTOR =
      new ThreadPoolExecutor(1, 1, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
  
  private static final ThreadPoolExecutor SERVER_EXECUTOR =
      new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());

  private static final Map<ZoePlatform, ThreadPoolExecutor> INFOCHANNEL_HELPER_THREAD =
      Collections.synchronizedMap(new EnumMap<ZoePlatform, ThreadPoolExecutor>(ZoePlatform.class));
  
  private static final ThreadPoolExecutor RANKED_MESSAGE_GENERATOR =
      new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());

  private static final Map<ZoePlatform, ThreadPoolExecutor> PLAYERS_DATA_EXECUTORS =
      Collections.synchronizedMap(new EnumMap<ZoePlatform, ThreadPoolExecutor>(ZoePlatform.class));
  
  private static final Map<ZoeRegion, ThreadPoolExecutor> MATCH_THREAD_EXECUTORS =
      Collections.synchronizedMap(new EnumMap<ZoeRegion, ThreadPoolExecutor>(ZoeRegion.class));
  
  private static final ThreadPoolExecutor LEADERBOARD_EXECUTOR =
      new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
  
  private static final ThreadPoolExecutor CLASH_CHANNEL_EXECUTOR =
      new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
  
  private static final ThreadPoolExecutor COMMANDS_EXECUTOR =
      new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());

  private static final ThreadPoolExecutor EVENTS_EXECUTOR =
      new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
  
  /**
   * Used by event waiter, define in {@link Zoe#main(String[])}
   */
  private static final ScheduledThreadPoolExecutor RESPONSE_WAITER = new ScheduledThreadPoolExecutor(NBR_PROC);
  
  private static boolean rebootAsked = false;

  private ServerThreadsManager() {
    // Hide public default constructor
  }

  static {
    logger.info("ThreadPools has been started with {} threads", NBR_PROC);
    DATA_ANALYSIS_MANAGER.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Analysis-Thread Manager").build());
    DATA_ANALYSIS_THREAD.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Analysis-Thread %d").build());
    SERVER_EXECUTOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Server-Executor-Thread %d").build());
    RANKED_MESSAGE_GENERATOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Ranked-Message-Generator-Thread %d").build());
    RESPONSE_WAITER.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Response-Waiter-Thread %d").build());
    LEADERBOARD_EXECUTOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Leaderboard-Refresher-Thread %d").build());
    CLASH_CHANNEL_EXECUTOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Clash-Channel-Executor-Thread %d").build());
    COMMANDS_EXECUTOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Command-Executor-Thread %d").build());
    MONITORING_DATA_EXECUTOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Data-Monitoring-Thread %d").build());
    EVENTS_EXECUTOR.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("Zoe Event-Executor-Thread %d").build());
    
    for(ZoePlatform platform : ZoePlatform.values()) {
      ThreadPoolExecutor executor = new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
      String nameOfThread = String.format("Zoe Infochannel-Helper-%s-Worker", platform.getShowableName());
      executor.setThreadFactory(new ThreadFactoryBuilder().setNameFormat(nameOfThread + " %d").build());
      INFOCHANNEL_HELPER_THREAD.put(platform, executor);
      
      executor = new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
      nameOfThread = String.format("Zoe Player-Data-%s-Worker", platform.getShowableName());
      executor.setThreadFactory(new ThreadFactoryBuilder().setNameFormat(nameOfThread + " %d").build());
      PLAYERS_DATA_EXECUTORS.put(platform, executor);
    }
    
    for(ZoeRegion region : ZoeRegion.values()) {
      ThreadPoolExecutor executor = new ThreadPoolExecutor(NBR_PROC, NBR_PROC, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
      String nameOfThread = String.format("Zoe Match-%s-Worker", region.getShowableName());
      executor.setThreadFactory(new ThreadFactoryBuilder().setNameFormat(nameOfThread + " %d").build());
      MATCH_THREAD_EXECUTORS.put(region, executor);
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
    RANKED_MESSAGE_GENERATOR.getQueue().clear();
    LEADERBOARD_EXECUTOR.getQueue().clear();
    COMMANDS_EXECUTOR.getQueue().clear();
    
    for(ZoePlatform platform : ZoePlatform.values()) {
      ThreadPoolExecutor playerWorker = INFOCHANNEL_HELPER_THREAD.get(platform);
      playerWorker.getQueue().clear();
    }
    
    for(ZoePlatform platform : ZoePlatform.values()) {
      ThreadPoolExecutor playerWorker = PLAYERS_DATA_EXECUTORS.get(platform);
      playerWorker.getQueue().clear();
    }
    
    for(ZoeRegion region : ZoeRegion.values()) {
      ThreadPoolExecutor matchWorker = MATCH_THREAD_EXECUTORS.get(region);
      matchWorker.getQueue().clear();
    }
    logger.info("All queue cleared !");
  }
  
  public static void shutDownTaskExecutor(TextChannel channel) throws InterruptedException {
    
    logger.info("Start to shutdown Data Analysis Thread, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Data Analysis Thread, this can take 1 minutes max...").complete();
    DATA_ANALYSIS_THREAD.shutdown();
    DATA_ANALYSIS_MANAGER.shutdown();

    DATA_ANALYSIS_THREAD.awaitTermination(1, TimeUnit.MINUTES);
    if(!DATA_ANALYSIS_THREAD.isTerminated()) {
      DATA_ANALYSIS_THREAD.shutdownNow();
    }
    
    if(!DATA_ANALYSIS_MANAGER.isTerminated()) {
      DATA_ANALYSIS_MANAGER.shutdown();
    }
    
    logger.info("Shutdown of Data Analysis Thread has been completed !");
    channel.sendMessage("Shutdown of Data Analysis Thread has been completed !").complete();

    logger.info("Start to shutdown Data Monitoring Executor, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Data Monitoring Executor, this can take 1 minutes max...").complete();
    MONITORING_DATA_EXECUTOR.shutdown();

    MONITORING_DATA_EXECUTOR.awaitTermination(1, TimeUnit.MINUTES);
    if(!MONITORING_DATA_EXECUTOR.isTerminated()) {
      MONITORING_DATA_EXECUTOR.shutdownNow();
    }
    logger.info("Shutdown of Data Monitoring Executor has been completed !");
    channel.sendMessage("Shutdown of Data Monitoring Executor has been completed !").complete();
    
    logger.info("Start to shutdown Response Waiter, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Response Waiter, this can take 1 minutes max...").complete();
    RESPONSE_WAITER.shutdown();

    RESPONSE_WAITER.awaitTermination(1, TimeUnit.MINUTES);
    if(!RESPONSE_WAITER.isTerminated()) {
      RESPONSE_WAITER.shutdownNow();
    }
    logger.info("Shutdown of Response Waiter has been completed !");
    channel.sendMessage("Shutdown of Response Waiter has been completed !").complete();
    
    logger.info("Start to shutdown Clash Executor, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Clash Executor, this can take 1 minutes max...").complete();
    CLASH_CHANNEL_EXECUTOR.shutdown();

    CLASH_CHANNEL_EXECUTOR.awaitTermination(1, TimeUnit.MINUTES);
    if(!CLASH_CHANNEL_EXECUTOR.isTerminated()) {
      CLASH_CHANNEL_EXECUTOR.shutdownNow();
    }
    logger.info("Shutdown of Clash Executor has been completed !");
    channel.sendMessage("Shutdown of Clash Executor has been completed !").complete();

    logger.info("Start to shutdown Servers Executor, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Servers Executor, this can take 1 minutes max...").complete();
    SERVER_EXECUTOR.shutdown();

    SERVER_EXECUTOR.awaitTermination(1, TimeUnit.MINUTES);
    if(!SERVER_EXECUTOR.isTerminated()) {
      SERVER_EXECUTOR.shutdownNow();
    }
    logger.info("Shutdown of Servers Executor has been completed !");
    channel.sendMessage("Shutdown of Servers Executor has been completed !").complete();

    logger.info("Start to shutdown Infochannel Helper Threads, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Infochannel Helper Threads, this can take 1 minutes max...").complete();
    
    for(ZoePlatform platform : ZoePlatform.values()) {
      ThreadPoolExecutor matchWorker = INFOCHANNEL_HELPER_THREAD.get(platform);
      matchWorker.shutdown();
      logger.info("Start to shutdown Infochannel Helper {}, this can take 1 minutes max...", platform.getShowableName());

      matchWorker.awaitTermination(1, TimeUnit.MINUTES);
      if(!matchWorker.isTerminated()) {
        matchWorker.shutdownNow();
      }
      logger.info("Shutdown of Infochannel Helper Threads has been completed !");
    }
    channel.sendMessage("Shutdown of Infochannel Helper Threads has been completed !").complete();
    
    logger.info("Start to shutdown Ranked Message Generator, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Ranked Message Generator, this can take 1 minutes max...").complete();
    RANKED_MESSAGE_GENERATOR.shutdown();

    RANKED_MESSAGE_GENERATOR.awaitTermination(1, TimeUnit.MINUTES);
    if(!RANKED_MESSAGE_GENERATOR.isTerminated()) {
      RANKED_MESSAGE_GENERATOR.shutdownNow();
    }
    logger.info("Shutdown of Ranked Message Generator has been completed !");
    channel.sendMessage("Shutdown of Ranked Message Generator has been completed !").complete();

    logger.info("Start to shutdown Players Data Worker...");
    channel.sendMessage("Start to shutdown Players Data Worker ...").complete();
    for(ZoePlatform platform : ZoePlatform.values()) {
      ThreadPoolExecutor playerWorker = PLAYERS_DATA_EXECUTORS.get(platform);
      playerWorker.shutdown();
      logger.info("Start to shutdown Players Worker {}, this can take 1 minutes max...", platform.getShowableName());
      
      playerWorker.awaitTermination(1, TimeUnit.MINUTES);
      if(!playerWorker.isTerminated()) {
        playerWorker.shutdownNow();
      }
      logger.info("Shutdown of Player Workers {} has been completed !", platform.getShowableName());
    }

    logger.info("Shutdown of Players Data Worker has been completed !");
    channel.sendMessage("Shutdown of Players Data Worker has been completed !").complete();

    channel.sendMessage("Start to shutdown Matchs Worker...").complete();
    for(ZoeRegion platform : ZoeRegion.values()) {
      ThreadPoolExecutor matchWorker = MATCH_THREAD_EXECUTORS.get(platform);
      matchWorker.shutdown();
      logger.info("Start to shutdown Match Worker {}, this can take 1 minutes max...", platform.getShowableName());

      matchWorker.awaitTermination(1, TimeUnit.MINUTES);
      if(!matchWorker.isTerminated()) {
        matchWorker.shutdownNow();
      }
      logger.info("Shutdown of Match Worker {} has been completed !", platform.getShowableName());
    }

    channel.sendMessage("Shutdown of Matchs Worker has been completed !").complete();
    
    logger.info("Start to shutdown Leaderboard Executor, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Leaderboard Executor, this can take 1 minutes max...").complete();
    LEADERBOARD_EXECUTOR.shutdown();

    LEADERBOARD_EXECUTOR.awaitTermination(1, TimeUnit.MINUTES);
    if(!LEADERBOARD_EXECUTOR.isTerminated()) {
      LEADERBOARD_EXECUTOR.shutdownNow();
    }
    logger.info("Shutdown of Leaderboard Executor has been completed !");
    channel.sendMessage("Shutdown of Leaderboard Executor has been completed !").complete();
    
    logger.info("Start to shutdown Events Executor, this can take 1 minutes max...");
    channel.sendMessage("Start to shutdown Events Executor, this can take 1 minutes max...").complete();
    EVENTS_EXECUTOR.shutdown();

    EVENTS_EXECUTOR.awaitTermination(1, TimeUnit.MINUTES);
    if(!EVENTS_EXECUTOR.isTerminated()) {
      EVENTS_EXECUTOR.shutdownNow();
    }
    logger.info("Shutdown of Events Executor has been completed !");
    channel.sendMessage("Shutdown of Events Executor has been completed !").complete();
    
    Runnable commandsShutDownRunnable = new Runnable() {
      
      @Override
      public void run() {
        logger.info("Start to shutdown Commands Executor, this can take 1 minutes max...");
        COMMANDS_EXECUTOR.shutdown();
        
        try {
          COMMANDS_EXECUTOR.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          logger.error("error while shutdowning commands Executor", e);
          Thread.currentThread().interrupt();
        }
        if(!COMMANDS_EXECUTOR.isTerminated()) {
          COMMANDS_EXECUTOR.shutdownNow();
        }

        logger.info("Shutdown of Commands Executor has been completed ! (shutdown will end at the end of this command)");
        Thread.currentThread().interrupt();
      }
    };
    
    Thread commandsShutDownThread = new Thread(commandsShutDownRunnable);
    commandsShutDownThread.start();
    

  }
  
  public static int getPlayersDataQueue() {
    int queueTotal = 0;
    for(ZoeRegion platform : ZoeRegion.values()) {
      queueTotal += MATCH_THREAD_EXECUTORS.get(platform).getQueue().size();
    }
    
    return queueTotal;
  }

  public static ThreadPoolExecutor getDataAnalysisManager() {
    return DATA_ANALYSIS_MANAGER;
  }

  public static ThreadPoolExecutor getDataAnalysisThread() {
    return DATA_ANALYSIS_THREAD;
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
  
  public static ThreadPoolExecutor getInfochannelHelperThread(ZoePlatform platform) {
    return INFOCHANNEL_HELPER_THREAD.get(platform);
  }

  public static ThreadPoolExecutor getPlayersDataWorker(ZoePlatform platform) {
    return PLAYERS_DATA_EXECUTORS.get(platform);
  }

  public static ScheduledThreadPoolExecutor getResponseWaiter() {
    return RESPONSE_WAITER;
  }

  public static ThreadPoolExecutor getMatchsWorker(ZoePlatform platform) {
    return MATCH_THREAD_EXECUTORS.get(ZoeRegion.getZoeRegionByPlatform(platform));
  }

  public static Timer getServerCheckerThreadTimer() {
    return serverCheckerThreadTimer;
  }

  public static ThreadPoolExecutor getRankedMessageGenerator() {
    return RANKED_MESSAGE_GENERATOR;
  }
  
  public static ThreadPoolExecutor getLeaderboardExecutor() {
    return LEADERBOARD_EXECUTOR;
  }
  
  public static ThreadPoolExecutor getCommandsExecutor() {
    return COMMANDS_EXECUTOR;
  }

  public static ThreadPoolExecutor getMonitoringDataExecutor() {
    return MONITORING_DATA_EXECUTOR;
  }

  public static Timer getDiscordDetectionDelayedTask() {
    return DISCORD_DETECTION_DELAYED_TASK;
  }

  public static ThreadPoolExecutor getClashChannelExecutor() {
    return CLASH_CHANNEL_EXECUTOR;
  }

  public static boolean isRebootAsked() {
    return rebootAsked;
  }

  public static void setRebootAsked(boolean rebootAsked) {
    ServerThreadsManager.rebootAsked = rebootAsked;
  }

  public static ThreadPoolExecutor getEventsExecutor() {
    return EVENTS_EXECUTOR;
  }
 

}
