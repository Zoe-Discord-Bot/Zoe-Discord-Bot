package ch.kalunight.zoe.service.leaderboard;

import java.sql.SQLException;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.Server;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.RiotApiException;

public class WinrateLeaderboardService extends LeaderboardBaseService {

  public WinrateLeaderboardService(long guildId, long channelId, long leaderboardId) {
    super(guildId, channelId, leaderboardId);
  }

  @Override
  protected void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel, Leaderboard leaderboard, Message message)
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
