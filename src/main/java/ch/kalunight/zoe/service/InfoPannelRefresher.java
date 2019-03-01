package ch.kalunight.zoe.service;

import ch.kalunight.zoe.model.Server;

public class InfoPannelRefresher implements Runnable {

  private Server server;

  public InfoPannelRefresher(Server server) {
    this.server = server;
  }

  @Override
  public void run() {
    if(server.getInfoChannel() != null) {
      
    }
  }

}
