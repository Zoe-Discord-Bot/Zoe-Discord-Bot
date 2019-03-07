package ch.kalunight.zoe.service;

import java.util.TimerTask;

import org.joda.time.DateTime;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import net.dv8tion.jda.core.entities.Guild;

public class GameChecker extends TimerTask {

  private static DateTime nextRefreshDate = DateTime.now();

  public static void setNextRefreshDate(DateTime nextRefreshDate) {
    GameChecker.nextRefreshDate = nextRefreshDate;
  }

  @Override
  public void run() {
    for(Guild guild : Zoe.getJda().getGuilds()) {
      if(guild.getOwnerId().equals(Zoe.getJda().getSelfUser().getId())) {
        continue;
      }
      Server server = ServerData.getServers().get(guild.getId());

      if(server == null) {
        server = new Server(guild, SpellingLangage.EN);
        ServerData.getServers().put(guild.getId(), server);
      }

      if(server.isNeedToBeRefreshed() && server.getInfoChannel() != null 
          && !ServerData.getServersIsInTreatment().get(guild.getId())) {

        Runnable task = new InfoPanelRefresher(server);
        ServerData.getTaskExecutor().submit(task);
      }
    }

    if(nextRefreshDate.isAfterNow()) {
      ServerData.getTaskExecutor().submit(new DataSaver());
      setNextRefreshDate(DateTime.now().plusMinutes(10));
    }
  }
}
