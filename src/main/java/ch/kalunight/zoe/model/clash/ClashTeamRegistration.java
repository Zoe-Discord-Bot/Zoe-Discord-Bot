package ch.kalunight.zoe.model.clash;

import net.rithms.riot.api.endpoints.clash.dto.ClashTeam;
import net.rithms.riot.api.endpoints.clash.dto.ClashTournament;

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