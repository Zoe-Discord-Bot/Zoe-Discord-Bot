package ch.kalunight.zoe.service.rankchannel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.RankHistoryChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.LastRankRepository;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.tft_match.dto.TFTMatch;

public class RankedChannelTFTRefresher extends RankedChannelBaseRefresher {

  public RankedChannelTFTRefresher(RankHistoryChannel rankChannel, LeagueEntry oldEntry, LeagueEntry newEntry, Player player,
      LeagueAccount leagueAccount, Server server) {
    super(rankChannel, oldEntry, newEntry, player, leagueAccount, server);
  }

  @Override
  protected void sendRankChangedWithoutBO() {
    sendTFTStandardMessage();
  }

  @Override
  protected void sendLeaguePointChangeOnly() {
    sendTFTStandardMessage();
  }

  private void sendTFTStandardMessage() {
    List<TFTMatch> matchs;
    try {
      matchs = getTFTMatchsSinceTheLastMessage();
    } catch(SQLException e) {
      logger.error("SQL error in RankedChannelTFTRefresher !", e);
      return;
    }
    
    MessageEmbed message;
    try {
      message = MessageBuilderRequest.createRankChannelCardLeaguePointChangeOnlyTFT
      (oldEntry, newEntry, matchs, player, leagueAccount, server.serv_language);
    } catch (NoValueRankException e) {
      logger.warn("Error while generating a TFT rank message!", e);
      return;
    }

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }

  private List<TFTMatch> getTFTMatchsSinceTheLastMessage() throws SQLException{
    List<TFTMatch> matchs = new ArrayList<>();

    LastRank lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);

    List<String> tftMatchsList = Zoe.getRiotApi()
        .getTFTMatchListWithRateLimit(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_puuid, 20);

    for(String matchId : tftMatchsList) {

      if(lastRank.lastRank_tftLastTreatedMatchId != null && matchId.equals(lastRank.lastRank_tftLastTreatedMatchId)) {
        break;
      }

      TFTMatch match = Zoe.getRiotApi().getTFTMatchWithRateLimit(leagueAccount.leagueAccount_server, matchId);

      if(match.getInfo().getQueueId() == GameQueueConfigId.RANKED_TFT.getId()) {
        matchs.add(match);
      }
    }

    if(!matchs.isEmpty() && lastRank.lastRank_tftLastTreatedMatchId == null) {
      LastRankRepository.updateLastRankTFTLastTreatedMatch(matchs.get(0).getMetadata().getMatchId(), leagueAccount.leagueAccount_id);
    }
    return matchs;
  }
  
  @Override
  protected void sendBOEnded() {
    //TFT doesn't handle this event
  }

  @Override
  protected void sendBOStarted() {
    //TFT doesn't handle this event
  }

  @Override
  protected void sendBOInProgess() {
    //TFT doesn't handle this event
  }

}
