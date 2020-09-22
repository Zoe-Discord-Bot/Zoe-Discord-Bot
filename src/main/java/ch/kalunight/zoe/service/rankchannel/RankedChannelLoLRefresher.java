package ch.kalunight.zoe.service.rankchannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.RankHistoryChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class RankedChannelLoLRefresher extends RankedChannelBaseRefresher {

  private CurrentGameInfo gameOfTheChange;

  public RankedChannelLoLRefresher(RankHistoryChannel rankChannel, LeagueEntry oldEntry, LeagueEntry newEntry,
      CurrentGameInfo gameOfTheChange,Player player, LeagueAccount leagueAccount, Server server) {
    super(rankChannel, oldEntry, newEntry, player, leagueAccount, server);
    this.gameOfTheChange = gameOfTheChange;
  }

  protected void sendRankChangedWithoutBO() {
    MessageEmbed message =
        MessageBuilderRequest.createRankChannelCardLeagueChange
        (oldEntry, newEntry, gameOfTheChange, player, leagueAccount, server.serv_language);

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }

  protected void sendBOEnded() {
    MessageEmbed message;
    try {
      message =
          MessageBuilderRequest.createRankChannelCardBoEnded(oldEntry, newEntry, gameOfTheChange,
              player, leagueAccount, server.serv_language);
    } catch(NoValueRankException e) {
      logger.error("Error when creating Rank Message", e);
      return;
    }

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }

  protected void sendBOStarted() {
    MessageEmbed message =
        MessageBuilderRequest.createRankChannelCardBoStarted(newEntry, gameOfTheChange, player, leagueAccount, 
            server.serv_language);

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }

  protected void sendBOInProgess() {
    MessageEmbed message =
        MessageBuilderRequest.createRankChannelBoInProgress(oldEntry, newEntry,
            gameOfTheChange, player,leagueAccount, server.serv_language);

    if(message != null) {
      TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
      if(textChannelWhereSend != null) {
        textChannelWhereSend.sendMessage(message).queue();
      }
    }
  }

  protected void sendLeaguePointChangeOnly() {
    MessageEmbed message = 
        MessageBuilderRequest.createRankChannelCardLeaguePointChangeOnly
        (oldEntry, newEntry, gameOfTheChange, player, leagueAccount, server.serv_language);

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }
}