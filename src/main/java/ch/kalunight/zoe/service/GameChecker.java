package ch.kalunight.zoe.service;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import net.dv8tion.jda.core.entities.Guild;

public class GameChecker implements Runnable {

  private static final int WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS = 10000;

  private static boolean needToBeShutDown = false;

  private static boolean shutdown = false;

  @Override
  public void run() {
    try {
      while(!needToBeShutDown) {
        for(Guild guild : Zoe.getJda().getGuilds()) {
          if(guild.getOwnerId().equals(Zoe.getJda().getSelfUser().getId())) {
            continue;
          }
          Server server = ServerData.getServers().get(guild.getId());

          if(server == null) {
            server = new Server(guild, SpellingLangage.EN);
            ServerData.getServers().put(guild.getId(), server);
          }

          if(server.isNeedToBeRefreshed() && server.getInfoChannel() != null) {
            Runnable task = new InfoPanelRefresher(server);
            ServerData.getTaskExecutor().submit(task);
          }
        }
        Thread.sleep(WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS);
      }
    } catch(InterruptedException e) {
      threadRecover();
      Thread.currentThread().interrupt();
    }finally {
      setShutdown(true);
    }
  }
  
  private void threadRecover() {
    Runnable gameChecker = new GameChecker();
    Thread thread = new Thread(gameChecker, "Game-Checker-Thread");
    thread.start();
  }

  public static boolean isNeedToBeShutDown() {
    return needToBeShutDown;
  }

  public static void setNeedToBeShutDown(boolean needToBeShutDown) {
    GameChecker.needToBeShutDown = needToBeShutDown;
  }

  public static boolean isShutdown() {
    return shutdown;
  }

  private static void setShutdown(boolean shutdown) {
    GameChecker.shutdown = shutdown;
  }
}
