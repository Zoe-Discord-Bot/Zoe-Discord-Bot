package ch.kalunight.zoe.service.leaderboard;

import java.sql.SQLException;
import java.util.List;

import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.Server;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.RiotApiException;

public class WinrateLeaderboardService extends LeaderboardBaseService {

  public WinrateLeaderboardService(long guildId, long channelId, long leaderboardId, boolean forceRefreshCache) {
    super(guildId, channelId, leaderboardId, forceRefreshCache);
  }

  @Override
  protected void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel, Leaderboard leaderboard, Message message, List<Player> players, boolean forceRefreshCache)
      throws SQLException, RiotApiException {
    
    /*Objective objective = Objective.getObjectiveWithId(leaderboard.lead_type);
    SpecificChamp selectedChamp = null;
    if(Objective.WINRATE_SPECIFIC_CHAMP.equals(objective)) {
      selectedChamp = gson.fromJson(leaderboard.lead_data, SpecificChamp.class);
    }
    
    QueueSelected queueSelected = null;
    if(Objective.WINRATE_SPECIFIC_QUEUE.equals(objective)) {
      queueSelected = gson.fromJson(leaderboard.lead_data, QueueSelected.class);
    }*/
    
    //TODO: implement leaderboard
    
  }

}
