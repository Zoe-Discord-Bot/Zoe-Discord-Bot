package ch.kalunight.zoe.model.dto;

import java.util.List;

import no.stelar7.api.r4j.pojo.lol.clash.ClashTournament;

public class ClashTournamentMongodb {

  private List<ClashTournament> tournaments;
  private String serverName;
  
  public ClashTournamentMongodb(List<ClashTournament> tournaments, ZoePlatform server) {
    this.tournaments = tournaments;
    this.serverName = server.getDbName();
  }
  
  public ClashTournament getTournamentById(int tournamentId) {
    for(ClashTournament tournament : tournaments) {
      if(tournament.getId() == tournamentId) {
        return tournament;
      }
    }
    return null;
  }
  
  public List<ClashTournament> getTournaments() {
    return tournaments;
  }
  
  public void setTournament(List<ClashTournament> tournament) {
    this.tournaments = tournament;
  }
  
  public String getServerName() {
    return serverName;
  }
  
  public void setServerName(ZoePlatform server) {
    this.serverName = server.getDbName();
  }
  
}
