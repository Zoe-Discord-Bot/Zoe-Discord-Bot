package ch.kalunight.zoe.model.dto;

import java.time.LocalDateTime;

import ch.kalunight.zoe.model.dto.DTO.Server;

public class DatedServer {

  private Server server;
  
  private LocalDateTime localDateTime;
  
  public DatedServer(Server server, LocalDateTime localDateTime) {
    this.server = server;
    this.localDateTime = localDateTime;
  }

  public Server getServer() {
    return server;
  }

  public void setServer(Server server) {
    this.server = server;
  }

  public LocalDateTime getLocalDateTime() {
    return localDateTime;
  }

  public void setLocalDateTime(LocalDateTime localDateTime) {
    this.localDateTime = localDateTime;
  }
  
}
