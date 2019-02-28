package ch.kalunight.zoe;

import java.util.HashMap;
import java.util.Map;
import ch.kalunight.zoe.model.Server;

public class ServerData {
  
  private static final HashMap<String, Server> servers = new HashMap<>();

  public static Map<String, Server> getServers() {
    return servers;
  }
  
}
