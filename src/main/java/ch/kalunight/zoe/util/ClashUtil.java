package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.util.List;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.clash.ClashTeamRegistration;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeam;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeamMember;
import net.rithms.riot.api.endpoints.clash.dto.ClashTournament;
import net.rithms.riot.constant.Platform;

public class ClashUtil {

  private ClashUtil() {
    // hide default public constructor
  }
  
  public static ClashTeamRegistration getFirstRegistration(Platform platform, List<ClashTeamMember> clashPlayerRegistrations, boolean forceCacheRefresh) throws RiotApiException, SQLException {

    ClashTeamRegistration teamRegistration = null;

    for(ClashTeamMember clashPlayer : clashPlayerRegistrations) {
      ClashTeam team = Zoe.getRiotApi().getClashTeamByTeamIdWithRateLimit(platform, clashPlayer.getTeamId());

      ClashTournament tournamentToCheck = Zoe.getRiotApi().getClashTournamentById(platform, team.getTournamentIdInt(), forceCacheRefresh);

      if(teamRegistration == null || teamRegistration.getTournament().getSchedule().get(0).getStartTime().isAfter(tournamentToCheck.getSchedule().get(0).getStartTime())) {
        teamRegistration = new ClashTeamRegistration(tournamentToCheck, team);
      }

    }
    return teamRegistration;
  }

}
