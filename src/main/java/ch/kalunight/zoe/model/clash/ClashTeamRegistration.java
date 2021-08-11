package ch.kalunight.zoe.model.clash;

import no.stelar7.api.r4j.pojo.lol.clash.ClashTeam;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTournament;

public class ClashTeamRegistration {
  private ClashTournament tournament;
  private ClashTeam team;

  public ClashTeamRegistration(ClashTournament tournament, ClashTeam team) {
    this.tournament = tournament;
    this.team = team;
  }

  public ClashTournament getTournament() {
    return tournament;
  }

  public void setTournament(ClashTournament tournament) {
    this.tournament = tournament;
  }

  public ClashTeam getTeam() {
    return team;
  }

  public void setTeam(ClashTeam team) {
    this.team = team;
  }
}