package ch.kalunight.zoe.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimerTask;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.repositories.LeaderboardRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.repositories.ServerStatusRepository;
import ch.kalunight.zoe.service.clashchannel.TreatClashChannel;
import ch.kalunight.zoe.service.leaderboard.LeaderboardBaseService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

public class ServerChecker extends TimerTask {

  private static final int TIME_BETWEEN_EACH_DISCORD_BOT_LIST_REFRESH = 10;

  private static final int TIME_BETWEEN_EACH_STATUS_REFRESH_IN_HOURS = 1;

  private static final int TIME_BETWEEN_EACH_RAPI_CHANNEL_REFRESH_IN_MINUTES = 2;
  
  private static TreatServerService serverRefreshService = null;

  private static DateTime nextDiscordBotListRefresh = DateTime.now().plusSeconds(TIME_BETWEEN_EACH_DISCORD_BOT_LIST_REFRESH);

  private static DateTime nextStatusRefresh = DateTime.now();

  private static DateTime nextRAPIChannelRefresh = DateTime.now().plusMinutes(TIME_BETWEEN_EACH_RAPI_CHANNEL_REFRESH_IN_MINUTES);

  private static final Logger logger = LoggerFactory.getLogger(ServerChecker.class);

  @Override
  public void run() {

    logger.debug("ServerChecker thread started !");

    try {
      
      if(serverRefreshService == null) {
        setServerRefreshService(new TreatServerService(ServerThreadsManager.getServerExecutor()));
      }

      logger.debug("Start to queue asked treatment server !");
      for(DTO.Server serverAskedTreatment : ServerThreadsManager.getServersAskedTreatment()) {
        DTO.ServerStatus status = ServerStatusRepository.getServerStatus(serverAskedTreatment.serv_guildId);
        if(!status.servstatus_inTreatment) {
          ServerStatusRepository.updateInTreatment(status.servstatus_id, true);
          ServerRepository.updateTimeStamp(serverAskedTreatment.serv_guildId, LocalDateTime.now());

          ServerChecker.getServerRefreshService().getServersAskedToRefresh().add(serverAskedTreatment);

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

      logger.debug("Start to refresh leaderboards !");
      refreshLeaderboard();

      logger.debug("Start to refresh clash channel !");
      refreshClashChannel();

      
      if(nextRAPIChannelRefresh.isBeforeNow() && RiotApiUsageChannelRefresh.getRapiInfoChannel() != null) {
        logger.info("Start to refresh riotAPI channel !");
        ServerThreadsManager.getDataAnalysisThread().execute(new RiotApiUsageChannelRefresh());

        setNextRAPIChannelRefresh(DateTime.now().plusMinutes(TIME_BETWEEN_EACH_RAPI_CHANNEL_REFRESH_IN_MINUTES));
      }

      if(nextDiscordBotListRefresh.isBeforeNow()) {
        logger.debug("Start to refresh discord bot stats !");
        if(Zoe.getBotListApi() != null) {
          for(JDA client : Zoe.getJDAs()) {
            Zoe.getBotListApi().setStats(client.getShardInfo().getShardId(), client.getShardInfo().getShardTotal(), (int) client.getGuildCache().size());
          }
        }

        setNextDiscordBotListRefresh(DateTime.now().plusMinutes(TIME_BETWEEN_EACH_DISCORD_BOT_LIST_REFRESH));
      }

      if(nextStatusRefresh.isBeforeNow()) {
        logger.debug("Start to refresh discord status !");
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
      logger.debug("ServerChecker thread ended !");
      ServerThreadsManager.getServerCheckerThreadTimer().schedule(new DataSaver(), 0);
      logger.debug("Zoe Server-Executor Queue : {}", ServerThreadsManager.getServerExecutor().getQueue().size());
      logger.debug("Zoe InfoCards-Generator Queue : {}", ServerThreadsManager.getInfocardsGenerator().getQueue().size());
      logger.debug("Zoe number of User cached : {}", Zoe.getNumberOfUsers());
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
  
  

  private static void setServerRefreshService(TreatServerService serverRefreshService) {
    ServerChecker.serverRefreshService = serverRefreshService;
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

  public static TreatServerService getServerRefreshService() {
    return serverRefreshService;
  }
}
