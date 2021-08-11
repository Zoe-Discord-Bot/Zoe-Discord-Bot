package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.util.List;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.clash.ClashTeamRegistration;
import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPlayer;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTeam;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTournament;

public class ClashUtil {

  private ClashUtil() {
    // hide default public constructor
  }
  
  public static ClashTeamRegistration getFirstRegistration(LeagueShard platform, List<ClashPlayer> clashPlayerRegistrations, boolean forceCacheRefresh) throws SQLException {

    ClashTeamRegistration teamRegistration = null;

    for(ClashPlayer clashPlayer : clashPlayerRegistrations) {
      ClashTeam team = Zoe.getRiotApi().getLoLAPI().getClashAPI().getTeam(platform, clashPlayer.getTeamId());

      ClashTournament tournamentToCheck = Zoe.getRiotApi().getLoLAPI().getClashAPI().getTournamentById(platform, team.getTournamentId());

      if(teamRegistration == null || teamRegistration.getTournament().getSchedule().get(0).getStartTimeAsDate().isAfter(tournamentToCheck.getSchedule().get(0).getStartTimeAsDate())) {
        teamRegistration = new ClashTeamRegistration(tournamentToCheck, team);
      }

    }
    return teamRegistration;
  }

}
