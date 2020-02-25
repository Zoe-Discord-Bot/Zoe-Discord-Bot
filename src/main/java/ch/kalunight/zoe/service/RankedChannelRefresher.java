package ch.kalunight.zoe.service;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.RankHistoryChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.repositories.LastRankRepository;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class RankedChannelRefresher implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(RankedChannelRefresher.class);

  private Server server;

  private RankHistoryChannel rankChannel;

  private LeagueEntry oldEntry;

  private LeagueEntry newEntry;

  private CurrentGameInfo gameOfTheChange;

  private Player player;

  private LeagueAccount leagueAccount;

  public RankedChannelRefresher(RankHistoryChannel rankChannel, LeagueEntry oldEntry, LeagueEntry newEntry, CurrentGameInfo gameOfTheChange,
      Player player, LeagueAccount leagueAccount, Server server) {
    this.rankChannel = rankChannel;
    this.oldEntry = oldEntry;
    this.newEntry = newEntry;
    this.gameOfTheChange = gameOfTheChange;
    this.player = player;
    this.leagueAccount = leagueAccount;
    this.server = server;
  }

  @Override
  public void run() {

    try {
      if(oldEntry != null) {
        
        FullTier oldFullTier = new FullTier(oldEntry);
        FullTier newFullTier = new FullTier(newEntry);

        if(oldFullTier.getLeaguePoints() == newFullTier.getLeaguePoints() || newFullTier.getLeaguePoints() == 100 
            || oldFullTier.getLeaguePoints() == 100) { //BO detected OR remake

          if(oldEntry.getMiniSeries() != null && newEntry.getMiniSeries() == null) { //BO ended
            sendBOEnded();
          }else if(oldEntry.getMiniSeries() == null && newEntry.getMiniSeries() != null) { //BO started
            sendBOStarted();
          }else if(oldEntry.getMiniSeries() != null && newEntry.getMiniSeries() != null) { //BO in progress OR remake
            int nbrMatchOldEntry = oldEntry.getMiniSeries().getLosses() + oldEntry.getMiniSeries().getWins();
            int nbrMatchNewEntry = newEntry.getMiniSeries().getLosses() + newEntry.getMiniSeries().getWins();

            if(nbrMatchNewEntry != nbrMatchOldEntry) { //BO in progress
              sendBOInProgess();
            }
          }

        }else { //No BO
          if(oldFullTier.getRank().equals(newFullTier.getRank()) 
              || oldFullTier.getTier().equals(newFullTier.getTier())) { //Only LP change
            sendLeaguePointChangeOnly();
          }else { //Decay OR division skip
            sendRankChangedWithoutBO();
          }
        }
      }
    } catch (Exception e) {
      logger.error("Unexpected exception in RankedChannelRefresher !", e);
    }

    try {
      LastRank lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);
      if(lastRank == null) {
        LastRankRepository.createLastRank(leagueAccount.leagueAccount_id);
        LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);
      }

      if(GameQueueConfigId.SOLOQ.getId() == gameOfTheChange.getGameQueueConfigId()) {
        LastRankRepository.updateLastRankSoloqWithLeagueAccountId(newEntry, leagueAccount.leagueAccount_id);
      }else if(GameQueueConfigId.FLEX.getId() == gameOfTheChange.getGameQueueConfigId()) {
        LastRankRepository.updateLastRankFlexWithLeagueAccountId(newEntry, leagueAccount.leagueAccount_id);
      }
    } catch (SQLException e) {
      logger.error("SQL error when refreshing last rank of a player", e);
    } catch (Exception e) {
      logger.error("Unexpected exception in RankedChannelRefresher !", e);
    }

  }

  private void sendRankChangedWithoutBO() {
    MessageEmbed message = 
        MessageBuilderRequest.createRankChannelCardLeagueChange
        (oldEntry, newEntry, gameOfTheChange, player, leagueAccount, server.serv_language);

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }

  private void sendBOEnded() {
    MessageEmbed message;
    try {
      message =
          MessageBuilderRequest.createRankChannelCardBoEnded(oldEntry, newEntry, gameOfTheChange, leagueAccount, server.serv_language);
    } catch(NoValueRankException e) {
      logger.error("Error when creating Rank Message", e);
      return;
    }

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }

  private void sendBOStarted() {
    MessageEmbed message =
        MessageBuilderRequest.createRankChannelCardBoStarted(newEntry, gameOfTheChange, player, leagueAccount, 
            server.serv_language);

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }

  private void sendBOInProgess() {
    MessageEmbed message =
        MessageBuilderRequest.createRankChannelBoInProgress(oldEntry, newEntry,
            gameOfTheChange, leagueAccount, server.serv_language);
    
    if(message != null) {
      TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
      if(textChannelWhereSend != null) {
        textChannelWhereSend.sendMessage(message).queue();
      }
    }
  }

  private void sendLeaguePointChangeOnly() {
    MessageEmbed message = 
        MessageBuilderRequest.createRankChannelCardLeaguePointChangeOnly
        (oldEntry, newEntry, gameOfTheChange, player, leagueAccount, server.serv_language);

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }

}
