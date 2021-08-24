package ch.kalunight.zoe.model.dto;

import java.util.List;

import no.stelar7.api.r4j.pojo.lol.clash.ClashTournament;

public class ClashTournamentMongodb {

  private List<ClashTournament> tournament;
  private String serverName;
  
  public ClashTournamentMongodb(List<ClashTournament> tournament, ZoePlatform server) {
    this.tournament = tournament;
    this.serverName = server.getDbName();
  }
  
  public List<ClashTournament> getTournament() {
    return tournament;
  }
  public void setTournament(List<ClashTournament> tournament) {
    this.tournament = tournament;
  }
  public String getServerName() {
    return serverName;
  }
  public void setServerName(ZoePlatform server) {
    this.serverName = server.getDbName();
  }
  
}
