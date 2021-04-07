package ch.kalunight.zoe.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.RefreshPhase;
import ch.kalunight.zoe.model.RefreshStatus;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.repositories.LeaderboardRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.repositories.ServerStatusRepository;
import ch.kalunight.zoe.service.clashchannel.TreatClashChannel;
import ch.kalunight.zoe.service.infochannel.InfoPanelRefresher;
import ch.kalunight.zoe.service.leaderboard.LeaderboardBaseService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

public class ServerChecker extends TimerTask {

  private static final int TIME_BETWEEN_EACH_DISCORD_BOT_LIST_REFRESH = 10;

  private static final int TIME_BETWEEN_EACH_STATUS_REFRESH_IN_HOURS = 1;

  private static final int TIME_BETWEEN_EACH_RAPI_CHANNEL_REFRESH_IN_MINUTES = 2;

  private static final int NUMBER_OF_TASK_MAX_DURING_EVALUATION = 100;

  private static final RefreshStatus lastStatus = new RefreshStatus();

  private static DateTime nextDiscordBotListRefresh = DateTime.now().plusSeconds(TIME_BETWEEN_EACH_DISCORD_BOT_LIST_REFRESH);

  private static DateTime nextStatusRefresh = DateTime.now();

  private static DateTime nextRAPIChannelRefresh = DateTime.now().plusMinutes(TIME_BETWEEN_EACH_RAPI_CHANNEL_REFRESH_IN_MINUTES);

  private static AtomicInteger sizeOfTheQueueAtStartOfLastCycle = new AtomicInteger();

  private static final Logger logger = LoggerFactory.getLogger(ServerChecker.class);

  private List<DTO.Server> manageRefreshRate() throws SQLException {

    int queueSize = ServerThreadsManager.getServerExecutor().getActiveCount() + ServerThreadsManager.getServerExecutor().getQueue().size();
    int numberOfManagerServer = PlayerRepository.getListDiscordIdOfRegisteredPlayers().size();
    int serverTreated = sizeOfTheQueueAtStartOfLastCycle.get() - queueSize;

    List<Server> serversToRefresh;

    switch (lastStatus.getRefreshPhase()) {
    case NEED_TO_INIT:
      List<Server> allServers = ServerRepository.getAllGuildTreatable();
      lastStatus.init(numberOfManagerServer, allServers);
      serversToRefresh = new ArrayList<>();
      break;
    case IN_EVALUATION_PHASE:
      lastStatus.manageEvaluationPhase(serverTreated);
      if(lastStatus.getRefreshPhase() == RefreshPhase.IN_EVALUATION_PHASE) {
        serversToRefresh = lastStatus.getServersToLoadInEvaluation(NUMBER_OF_TASK_MAX_DURING_EVALUATION - queueSize);
        break;
      }
      serversToRefresh = new ArrayList<>();
      break;
    case IN_EVALUATION_PHASE_ON_ROAD:
      lastStatus.manageEvaluationPhaseOnRoad(numberOfManagerServer, queueSize, serverTreated);
      serversToRefresh = ServerRepository.getGuildWhoNeedToBeRefresh(lastStatus.getRefresRatehInMinute().get());
      break;
    case CLASSIC_MOD:
      lastStatus.manageClassicMod(numberOfManagerServer, queueSize, serverTreated);
      serversToRefresh = ServerRepository.getGuildWhoNeedToBeRefresh(lastStatus.getRefresRatehInMinute().get());
      break;
    case SMART_MOD:
      lastStatus.manageSmartMod(numberOfManagerServer, serverTreated);
      serversToRefresh = new ArrayList<>();
      break;
    default:
      serversToRefresh = new ArrayList<>();
      break;
    }

    sizeOfTheQueueAtStartOfLastCycle.set(queueSize + serversToRefresh.size());

    return serversToRefresh;
  }

  @Override
  public void run() {

    logger.info("ServerChecker thread started !");

    try {

      logger.info("Manage RefreshRate started !");
      List<DTO.Server> serversToRefresh = manageRefreshRate();

      logger.info("Start to queue {} servers !", serversToRefresh.size());
      for(DTO.Server server : serversToRefresh) {
        DTO.ServerStatus status = ServerStatusRepository.getServerStatus(server.serv_guildId);
        ServerStatusRepository.updateInTreatment(status.servstatus_id, true);
        ServerRepository.updateTimeStamp(server.serv_guildId, LocalDateTime.now());

        Runnable task = new InfoPanelRefresher(server, false);
        ServerThreadsManager.getServerExecutor().execute(task);
      }

      logger.info("Start to queue asked treatment server !");
      for(DTO.Server serverAskedTreatment : ServerThreadsManager.getServersAskedTreatment()) {
        DTO.ServerStatus status = ServerStatusRepository.getServerStatus(serverAskedTreatment.serv_guildId);
        if(!status.servstatus_inTreatment) {
          ServerStatusRepository.updateInTreatment(status.servstatus_id, true);
          ServerRepository.updateTimeStamp(serverAskedTreatment.serv_guildId, LocalDateTime.now());

          Runnable task = new InfoPanelRefresher(serverAskedTreatment, true);
          ServerThreadsManager.getServerExecutor().execute(task);

          List<Leaderboard> leaderboards = LeaderboardRepository.getLeaderboardsWithGuildId(serverAskedTreatment.serv_guildId);
          for(Leaderboard leaderboard : leaderboards) {
            LeaderboardRepository.updateLeaderboardLastRefreshWithLeadId(leaderboard.lead_id, LocalDateTime.now());

            LeaderboardBaseService leaderboardRefreshService = 
                LeaderboardBaseService.getServiceWithObjective(Objective.getObjectiveWithId(leaderboard.lead_type),
                    serverAskedTreatment.serv_guildId, leaderboard.lead_message_channelId, leaderboard.lead_id, true);

            if(leaderboardRefreshService != null) {
              ServerThreadsManager.getLeaderboardExecutor().execute(leaderboardRefreshService);
            }else {
              logger.error("Impossible to get the service correspondig to the objective id {} !", leaderboard.lead_type);
            }
          }
        }
      }

      ServerThreadsManager.getServersAskedTreatment().clear();

      logger.info("Start to refresh leaderboards !");
      refreshLeaderboard();

      logger.info("Start to refresh clash channel !");
      refreshClashChannel();

      
      if(nextRAPIChannelRefresh.isBeforeNow() && RiotApiUsageChannelRefresh.getRapiInfoChannel() != null) {
        logger.info("Start to refresh riotAPI channel !");
        ServerThreadsManager.getDataAnalysisThread().execute(new RiotApiUsageChannelRefresh());

        setNextRAPIChannelRefresh(DateTime.now().plusMinutes(TIME_BETWEEN_EACH_RAPI_CHANNEL_REFRESH_IN_MINUTES));
      }

      if(nextDiscordBotListRefresh.isBeforeNow()) {
        logger.info("Start to refresh discord bot stats !");
        if(Zoe.getBotListApi() != null) {
          for(JDA client : Zoe.getJDAs()) {
            Zoe.getBotListApi().setStats(client.getShardInfo().getShardId(), client.getShardInfo().getShardTotal(), (int) client.getGuildCache().size());
          }
        }

        setNextDiscordBotListRefresh(DateTime.now().plusMinutes(TIME_BETWEEN_EACH_DISCORD_BOT_LIST_REFRESH));
      }

      if(nextStatusRefresh.isBeforeNow()) {
        logger.info("Start to refresh discord status !");
        // Discord status
        for(JDA client : Zoe.getJDAs()) {
          client.getPresence().setStatus(OnlineStatus.ONLINE);
          client.getPresence().setActivity(Activity.playing("type \">help\""));
        }

        setNextStatusRefresh(nextStatusRefresh.plusHours(TIME_BETWEEN_EACH_STATUS_REFRESH_IN_HOURS));
      }
    }catch(SQLException e) {
      logger.error("Critical DB Issue in the server checker thread ! SQL State : {}", e.getSQLState(), e);
    }catch(Exception e){
      logger.error("Unexpected error in ServerChecker", e);
    }finally {
      logger.info("ServerChecker thread ended !");
      ServerThreadsManager.getServerCheckerThreadTimer().schedule(new DataSaver(), 0);
      logger.info("Zoe Server-Executor Queue : {}", ServerThreadsManager.getServerExecutor().getQueue().size());
      logger.info("Zoe InfoCards-Generator Queue : {}", ServerThreadsManager.getInfocardsGenerator().getQueue().size());
      logger.info("Zoe number of User cached : {}", Zoe.getNumberOfUsers());
    }
  }

  private void refreshClashChannel() throws SQLException {
    List<ClashChannel> clashChannelsToRefresh = ClashChannelRepository.getClashChannelWhoNeedToBeRefreshed();

    for(ClashChannel clashChannelToRefresh : clashChannelsToRefresh) {
      Server server = ServerRepository.getServerWithServId(clashChannelToRefresh.clashChannel_fk_server);

      ClashChannelRepository.updateClashChannelRefresh(LocalDateTime.now(), clashChannelToRefresh.clashChannel_id);

      TreatClashChannel clashChannelWorker = new TreatClashChannel(server, clashChannelToRefresh, false);

      ServerThreadsManager.getClashChannelExecutor().execute(clashChannelWorker);
    }
  }

  private void refreshLeaderboard() throws SQLException {
    List<Leaderboard> leaderboardsToRefresh = LeaderboardRepository.getLeaderboardWhoNeedToBeRefreshed();

    for(Leaderboard leaderboardToRefresh : leaderboardsToRefresh) {
      Server server = ServerRepository.getServerWithServId(leaderboardToRefresh.lead_fk_server);

      LeaderboardRepository.updateLeaderboardLastRefreshWithLeadId(leaderboardToRefresh.lead_id, LocalDateTime.now());

      LeaderboardBaseService leaderboardRefreshService = 
          LeaderboardBaseService.getServiceWithObjective(Objective.getObjectiveWithId(leaderboardToRefresh.lead_type),
              server.serv_guildId, leaderboardToRefresh.lead_message_channelId, leaderboardToRefresh.lead_id, false);

      if(leaderboardRefreshService != null) {
        ServerThreadsManager.getLeaderboardExecutor().execute(leaderboardRefreshService);
      }else {
        logger.error("Impossible to get the service correspondig to the objective id {} !", leaderboardToRefresh.lead_type);
      }
    }
  }

  public static RefreshStatus getLastStatus() {
    return lastStatus;
  }

  public static void setNextStatusRefresh(DateTime nextStatusRefresh) {
    ServerChecker.nextStatusRefresh = nextStatusRefresh;
  }

  public static void setNextDiscordBotListRefresh(DateTime nextRefreshDate) {
    ServerChecker.nextDiscordBotListRefresh = nextRefreshDate;
  }

  private static void setNextRAPIChannelRefresh(DateTime nextRAPIChannelRefresh) {
    ServerChecker.nextRAPIChannelRefresh = nextRAPIChannelRefresh;
  }
}
