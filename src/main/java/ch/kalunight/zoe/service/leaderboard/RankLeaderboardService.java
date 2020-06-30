package ch.kalunight.zoe.service.leaderboard;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.PlayerPoints;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;

public class RankLeaderboardService extends LeaderboardBaseService {

  public RankLeaderboardService(long guildId, long channelId, long leaderboardId) {
    super(guildId, channelId, leaderboardId);
  }

  @Override
  protected void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel, Leaderboard leaderboard,
      Message message) throws SQLException, RiotApiException {
    
    
    
  }

  private List<PlayerPoints> orderAndGetPlayers(Guild guild, int championId) throws SQLException, RiotApiException {
    List<DTO.Player> players = PlayerRepository.getPlayers(guild.getIdLong());
    List<PlayerPoints> playersPoints = new ArrayList<>();
    
    for(DTO.Player player : players) {
      List<LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithPlayerID(guild.getIdLong(), player.player_id);
      
      long bestAccountRank = 0;
      for(DTO.LeagueAccount leagueAccount : leaguesAccounts) {
        ChampionMastery mastery = Zoe.getRiotApi().getChampionMasteriesBySummonerByChampionWithRateLimit(leagueAccount.leagueAccount_server,
            leagueAccount.leagueAccount_summonerId, championId);
        
        if(mastery == null) {
          continue;
        }
        
        if(bestAccountRank < mastery.getChampionPoints()) {
          bestAccountRank = mastery.getChampionPoints();
        }
      }
      playersPoints.add(new PlayerPoints(player, bestAccountRank));
    }
    
    Collections.sort(playersPoints);
    
    return playersPoints;
  }
  
}
