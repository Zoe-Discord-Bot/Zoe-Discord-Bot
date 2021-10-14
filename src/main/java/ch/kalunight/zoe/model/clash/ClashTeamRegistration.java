package ch.kalunight.zoe.model.clash;

import ch.kalunight.zoe.model.dto.SavedClashTournament;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTeam;

public class ClashTeamRegistration {
  private SavedClashTournament tournament;
  private ClashTeam team;

  public ClashTeamRegistration(SavedClashTournament tournament, ClashTeam team) {
    this.tournament = tournament;
    this.team = team;
  }

  public SavedClashTournament getTournament() {
    return tournament;
  }

  public void setTournament(SavedClashTournament tournament) {
    this.tournament = tournament;
  }

  public ClashTeam getTeam() {
    return team;
  }

  public void setTeam(ClashTeam team) {
    this.team = team;
  }
}