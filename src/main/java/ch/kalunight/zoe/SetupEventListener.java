package ch.kalunight.zoe;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.repositories.ServerStatusRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SetupEventListener extends ListenerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(SetupEventListener.class);
  
  @Override
  public void onReady(ReadyEvent event) {
    event.getJDA().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
    event.getJDA().getPresence().setActivity(Activity.playing("Booting ..."));

    logger.info("Setup non initialized Guild for shard {} ...", event.getJDA().getShardInfo().getShardId());
    try {
      setupNonInitializedGuild(event.getJDA());
    } catch(SQLException e) {
      logger.error("Issue when setup non initialized Guild !", e);
      System.exit(1);
    }
    logger.info("Setup non initialized Guild Done !");
    
    event.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
    event.getJDA().getPresence().setActivity(Activity.playing("type \">help\""));
    
    logger.info("Shard {} as booted correctly !", event.getJDA().getShardInfo().getShardId());
  }
  
  private void setupNonInitializedGuild(JDA jda) throws SQLException {
    for(Guild guild : jda.getGuilds()) {
      if(!ServerRepository.checkServerExist(guild.getIdLong())) {
        ServerRepository.createNewServer(guild.getIdLong(), LanguageManager.DEFAULT_LANGUAGE);
      }
      
      EventListener.getServersConfig().put(guild.getIdLong(), ConfigRepository.getServerConfiguration(guild.getIdLong(), jda));
    }
    ServerStatusRepository.updateAllServerInTreatment(false);
  }

  
}
