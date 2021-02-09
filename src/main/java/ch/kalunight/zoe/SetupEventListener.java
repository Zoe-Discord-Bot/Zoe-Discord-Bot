package ch.kalunight.zoe;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.discordbots.api.client.DiscordBotListAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.repositories.ServerStatusRepository;
import ch.kalunight.zoe.riotapi.CacheManager;
import ch.kalunight.zoe.service.RiotApiUsageChannelRefresh;
import ch.kalunight.zoe.service.ServerChecker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.EventListenerUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SetupEventListener extends ListenerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(SetupEventListener.class);
  
  private static boolean zoeIsBooted = false;
  
  private EventWaiter waiter;
  
  public SetupEventListener(EventWaiter waiter) {
    this.waiter = waiter;
  }
  
  @Override
  public void onReady(ReadyEvent event) {
    event.getJDA().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
    event.getJDA().getPresence().setActivity(Activity.playing("Booting ..."));

    logger.info("Setup non initialized Guild ...");
    try {
      setupNonInitializedGuild(event.getJDA());
    } catch(SQLException e) {
      logger.error("Issue when setup non initialized Guild !", e);
      System.exit(1);
    }
    logger.info("Setup non initialized Guild Done !");

    logger.info("Loading of champions ...");
    try {
      Zoe.loadChampions();
    } catch(IOException e) {
      logger.error("Critical error with the loading of champions !", e);
      System.exit(1);
    }

    logger.info("Loading of champions finished !");

    logger.info("Loading of emotes ...");
    try {
      EventListenerUtil.loadCustomEmotes();
      logger.info("Loading of emotes finished !");
    } catch(IOException e) {
      logger.warn("Error with the loading of emotes : {}", e.getMessage());
    }

    logger.info("Setup cache ...");
    CacheManager.setupCache();
    logger.info("Setup cache finished !");

    logger.info("Loading of RAPI Status Channel ...");

    initRAPIStatusChannel();

    logger.info("Loading of RAPI Status Channel finished !");

    logger.info("Loading of DiscordBotList API ...");

    try {
      Zoe.setBotListApi(new DiscordBotListAPI.Builder().botId(event.getJDA().getSelfUser().getId()).token(Zoe.getDiscordBotListTocken()) // SET
          .build());                                                                                                                   // TOCKEN

      logger.info("Loading of DiscordBotList API finished !");
    } catch(Exception e) {
      logger.info("Discord bot list api not loaded normally ! Working of the bot not affected");
      Zoe.setBotListApi(null);
    }

    logger.info("Setup of main thread  ...");
    setupContinousRefreshThread();
    logger.info("Setup of main thread finished !");
    
    logger.info("Setup of commands ...");
    event.getJDA().addEventListener(waiter);
    logger.info("Setup of commands done !");
    
    logger.info("Setup of EventListener ...");
    EventListener eventListener = new EventListener();
    event.getJDA().addEventListener(eventListener);
    logger.info("Setup of EventListener done !");
    
    event.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
    event.getJDA().getPresence().setActivity(Activity.playing("type \">help\""));

    PlayerRepository.getLoadedGuild().clear();
    
    setZoeIsBooted(true);
    
    logger.info("Booting finished !");
  }
  
  private void setupNonInitializedGuild(JDA jda) throws SQLException {
    for(Guild guild : jda.getGuilds()) {
      if(!guild.getOwnerId().equals(jda.getSelfUser().getId()) && !ServerRepository.checkServerExist(guild.getIdLong())) {
        ServerRepository.createNewServer(guild.getIdLong(), LanguageManager.DEFAULT_LANGUAGE);
      }
    }
    ServerStatusRepository.updateAllServerInTreatment(false);
  }

  private void setupContinousRefreshThread() {
    TimerTask mainThread = new ServerChecker();
    ServerThreadsManager.getServerCheckerThreadTimer().schedule(mainThread, 10000);
  }

  private void initRAPIStatusChannel() {
    try(final BufferedReader reader = new BufferedReader(new FileReader(Zoe.RAPI_SAVE_TXT_FILE));) {
      String line;

      List<String> args = new ArrayList<>();

      while((line = reader.readLine()) != null) {
        args.add(line);
      }

      if(args.size() == 2) {
        Guild guild = Zoe.getGuildById(args.get(0));
        if(guild != null) {
          TextChannel rapiStatusChannel = guild.getTextChannelById(args.get(1));
          if(rapiStatusChannel != null) {
            RiotApiUsageChannelRefresh.setTextChannelId(rapiStatusChannel.getIdLong());
            RiotApiUsageChannelRefresh.setGuildId(guild.getIdLong());
            logger.info("RAPI Status channel correctly loaded.");
          }
        }
      }      
    } catch(FileNotFoundException e1) {
      logger.info("Needed file doesn't exist. Will be created if needed.");
    } catch(IOException e1) {
      logger.warn("Error when loading the file of RAPI Status Channel. The older channel will be unused ! (You can re-create it)");
    }
  }

  public static boolean isZoeIsBooted() {
    return zoeIsBooted;
  }

  public static void setZoeIsBooted(boolean zoeIsBooted) {
    SetupEventListener.zoeIsBooted = zoeIsBooted;
  }

  
}
