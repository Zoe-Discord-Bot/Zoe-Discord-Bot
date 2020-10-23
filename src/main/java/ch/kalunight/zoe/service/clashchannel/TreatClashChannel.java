package ch.kalunight.zoe.service.clashchannel;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.ClashTeamMessageManager;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeam;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeamMember;
import net.rithms.riot.api.endpoints.clash.dto.ClashTournament;
import net.rithms.riot.constant.Platform;

public class TreatClashChannel implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(TreatClashChannel.class);
  
  private DTO.Server server;
  
  private DTO.ClashChannel clashChannel;
  
  public TreatClashChannel(Server server, ClashChannel clashChannel) {
    this.server = server;
    this.clashChannel = clashChannel;
  }
  
  @Override
  public void run() {
    try {
      
      ClashTeamMessageManager clashMessageManager = clashChannel.clashChannel_teamMessages;
      
      DTO.LeagueAccount leagueAccount = LeagueAccountRepository.getLeagueAccountWithLeagueAccountId(clashMessageManager.getSelectedLeagueAccountId());
      
      List<ClashTeamMember> clashPlayerRegistrations = Zoe.getRiotApi().getClashPlayerBySummonerIdWithRateLimit(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);
      
      ClashTeam firstClashTeam = getFirstRegitration(leagueAccount.leagueAccount_server, clashPlayerRegistrations);
      
      
      
    }catch(Exception e) {
      logger.error("Error while loading error");
    }
  }

  private ClashTeam getFirstRegitration(Platform platform, List<ClashTeamMember> clashPlayerRegistrations) throws RiotApiException {

    ClashTournament firstTournament = null;
    ClashTeam firstTeam = null;
    
    for(ClashTeamMember clashPlayer : clashPlayerRegistrations) {
      ClashTeam team = Zoe.getRiotApi().getClashTeamByTeamIdWithRateLimit(platform, clashPlayer.getTeamId());
      
      ClashTournament tournamentToCheck = Zoe.getRiotApi().getClashTournamentById(platform, team.getTournamentId());
      
      if(firstTournament == null || firstTournament.getSchedule().get(0).getStartTime().isAfter(tournamentToCheck.getSchedule().get(0).getStartTime())) {
        firstTournament = tournamentToCheck;
        firstTeam = team;
      }
      
    }
    return firstTeam;
  }
}
