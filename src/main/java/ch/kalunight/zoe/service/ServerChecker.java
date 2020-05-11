package ch.kalunight.zoe.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.TimerTask;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.repositories.ServerStatusRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

public class ServerChecker extends TimerTask {

  private static final int TIME_BETWEEN_EACH_DISCORD_BOT_LIST_REFRESH = 10;

  private static final int TIME_BETWEEN_EACH_STATUS_REFRESH_IN_HOURS = 1;

  private static final int TIME_BETWEEN_EACH_RAPI_CHANNEL_REFRESH_IN_MINUTES = 2;

  private static DateTime nextDiscordBotListRefresh = DateTime.now().plusSeconds(TIME_BETWEEN_EACH_DISCORD_BOT_LIST_REFRESH);

  private static DateTime nextStatusRefresh = DateTime.now();

  private static DateTime nextRAPIChannelRefresh = DateTime.now().plusMinutes(TIME_BETWEEN_EACH_RAPI_CHANNEL_REFRESH_IN_MINUTES);

  private static final Logger logger = LoggerFactory.getLogger(ServerChecker.class);

  @Override
  public void run() {

    logger.debug("ServerChecker thread started !");
    boolean hasReboot = false;

    try {

      hasReboot = checkIfRebootOfJDANeeded();

      if(hasReboot) {
        logger.info("Zoe is rebooting ...");
        return;
      }
      
      for(DTO.Server server : ServerRepository.getGuildWhoNeedToBeRefresh()) {

        DTO.ServerStatus status = ServerStatusRepository.getServerStatus(server.serv_guildId);
        ServerStatusRepository.updateInTreatment(status.servstatus_id, true);
        ServerRepository.updateTimeStamp(server.serv_guildId, LocalDateTime.now());

        Runnable task = new InfoPanelRefresher(server);
        ServerData.getServerExecutor().execute(task);
      }

      for(DTO.Server serverAskedTreatment : ServerData.getServersAskedTreatment()) {
        DTO.ServerStatus status = ServerStatusRepository.getServerStatus(serverAskedTreatment.serv_guildId);
        if(!status.servstatus_inTreatment) {
          ServerStatusRepository.updateInTreatment(status.servstatus_id, true);
          ServerRepository.updateTimeStamp(serverAskedTreatment.serv_guildId, LocalDateTime.now());

          Runnable task = new InfoPanelRefresher(serverAskedTreatment);
          ServerData.getServerExecutor().execute(task);
        }
      }

      ServerData.getServersAskedTreatment().clear();

      if(nextRAPIChannelRefresh.isBeforeNow() && RiotApiUsageChannelRefresh.getRapiInfoChannel() != null) {
        ServerData.getServerExecutor().execute(new RiotApiUsageChannelRefresh());

        setNextRAPIChannelRefresh(DateTime.now().plusMinutes(TIME_BETWEEN_EACH_RAPI_CHANNEL_REFRESH_IN_MINUTES));
      }

      if(nextDiscordBotListRefresh.isBeforeNow()) {

        if(Zoe.getBotListApi() != null) {
          // Discord bot list status
          Zoe.getBotListApi().setStats(Zoe.getJda().getGuilds().size());
        }

        setNextDiscordBotListRefresh(DateTime.now().plusMinutes(TIME_BETWEEN_EACH_DISCORD_BOT_LIST_REFRESH));
      }

      if(nextStatusRefresh.isBeforeNow()) {
        // Discord status
        Zoe.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
        Zoe.getJda().getPresence().setActivity(Activity.playing("type \">help\""));

        setNextStatusRefresh(nextStatusRefresh.plusHours(TIME_BETWEEN_EACH_STATUS_REFRESH_IN_HOURS));
      }
    }catch(SQLException e) {
      logger.error("Critical DB Issue in the server checker thread !", e);
    }catch(Exception e){
      logger.error("Unexpected error in ServerChecker", e);
    }finally {
      logger.debug("ServerChecker thread ended !");
      if(!hasReboot) {
        ServerData.getServerCheckerThreadTimer().schedule(new DataSaver(), 0);
        logger.debug("Zoe Server-Executor Queue : {}", ServerData.getServerExecutor().getQueue().size());
        logger.debug("Zoe InfoCards-Generator Queue : {}", ServerData.getInfocardsGenerator().getQueue().size());
      }
    }
  }

  private boolean checkIfRebootOfJDANeeded() {
    JDA jda = Zoe.getJda();
    
    if(checkIfZoeNeedReboot(jda) || ServerData.isRebootAsked()) {
      logger.info("Zoe is deconnected from Discord server ! Reboot thread start ...");
      ServerData.setRebootAsked(false);
      jda.shutdownNow();
      
      TimerTask rebootTask = new ZoeRebootThread();
      ServerData.getServerCheckerThreadTimer().schedule(rebootTask, 100);
      
      return true;
    }
    return false;
  }

  private boolean checkIfZoeNeedReboot(JDA jda) {
    return !jda.getStatus().equals(Status.CONNECTED) || 
        (!jda.getPresence().getStatus().equals(OnlineStatus.ONLINE) 
            && !jda.getPresence().getStatus().equals(OnlineStatus.DO_NOT_DISTURB))
        || jda.getEventManager().getRegisteredListeners().size() != Zoe.getEventlistenerlist().size();
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
