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
import net.rithms.riot.constant.Platform;

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
        .getTFTMatchListWithRateLimit(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_tftPuuid, 20);

    for(String matchId : tftMatchsList) {

      TFTMatch match = Zoe.getRiotApi().getTFTMatchWithRateLimit(leagueAccount.leagueAccount_server, matchId);

      if(match.getInfo().getQueueId() == GameQueueConfigId.RANKED_TFT.getId()) {
        matchs.add(match);
      }
    }
    
    if(lastRank.lastRank_tftLastTreatedMatchId != null) {
      matchs = getMatchsAfterLastGame(matchs, lastRank.lastRank_tftLastTreatedMatchId, leagueAccount.leagueAccount_server);
    }

    if(!matchs.isEmpty()) {
      
      TFTMatch lastRankedMatch = getLatestMatch(matchs);
      
      if(lastRankedMatch != null) {
        
        if(lastRank.lastRank_tftLastTreatedMatchId == null) {
          matchs.clear();
          matchs.add(lastRankedMatch);
        }
        
        LastRankRepository.updateLastRankTFTLastTreatedMatch(lastRankedMatch.getMetadata().getMatchId(), lastRank);
      }
    }
    return matchs;
  }

  private List<TFTMatch> getMatchsAfterLastGame(List<TFTMatch> matchs, String lastTreatedGameID, Platform platform) {
    
    List<TFTMatch> matchsAfterTheGame = new ArrayList<>();
    
    TFTMatch lastTreatedMatch = null;
    for(TFTMatch match : matchs) {
      if(match.getMetadata().getMatchId().equals(lastTreatedGameID)) {
        lastTreatedMatch = match;
        break;
      }
    }
    
    if(lastTreatedMatch == null) {
      return matchs;
    }
    
    for(TFTMatch match : matchs) {
      if(lastTreatedMatch.getInfo().getGameDateTime() < match.getInfo().getGameDateTime()) {
        matchsAfterTheGame.add(match);
      }
    }
    return matchsAfterTheGame;
  }

  private TFTMatch getLatestMatch(List<TFTMatch> matchs) {
    TFTMatch lastRankedMatch = null;
    
    for(TFTMatch match : matchs) {
      if(lastRankedMatch == null || lastRankedMatch.getInfo().getGameDateTime() < match.getInfo().getGameDateTime()) {
        lastRankedMatch = match;
      }
    }
    return lastRankedMatch;
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
