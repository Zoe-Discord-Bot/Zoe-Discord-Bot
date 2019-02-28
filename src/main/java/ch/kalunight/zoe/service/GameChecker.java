package ch.kalunight.zoe.service;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.ZoeMain;
import ch.kalunight.zoe.model.Server;
import net.dv8tion.jda.core.entities.Guild;

public class GameChecker implements Runnable {

  private static boolean needToBeShutDown = false;
  
  @Override
  public void run() {
    while(!needToBeShutDown) {
      for(Guild guild : ZoeMain.getJda().getGuilds()) {
        Server server = ServerData.getServers().get(guild.getId());
        
        if(server.isNeedToBeRefreshed() && server.getInfoChannel() != null) {
          Runnable task = new InfoPannelRefresher(server);
          ServerData.getTaskExecutor().submit(task);
        }
      }
    }
  }

  public static boolean isNeedToBeShutDown() {
    return needToBeShutDown;
  }

  public static void setNeedToBeShutDown(boolean needToBeShutDown) {
    GameChecker.needToBeShutDown = needToBeShutDown;
  }

}
