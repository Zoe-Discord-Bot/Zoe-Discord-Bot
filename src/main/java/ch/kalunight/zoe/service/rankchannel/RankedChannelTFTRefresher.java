package ch.kalunight.zoe.service.rankchannel;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.RankHistoryChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;
import no.stelar7.api.r4j.pojo.tft.TFTMatch;

public class RankedChannelTFTRefresher extends RankedChannelBaseRefresher {

  private TFTMatch match;
  
  public RankedChannelTFTRefresher(RankHistoryChannel rankChannel, LeagueEntry oldEntry, LeagueEntry newEntry, Player player,
      LeagueAccount leagueAccount, Server server, TFTMatch match, JDA jda) {
    super(rankChannel, oldEntry, newEntry, player, leagueAccount, server, jda);
    this.match = match;
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
    
    MessageEmbed message;
    try {
      message = MessageBuilderRequest.createRankChannelCardLeaguePointChangeOnlyTFT
      (oldEntry, newEntry, match, player, leagueAccount, server.getLanguage(), jda);
    } catch (NoValueRankException | APIResponseException e) {
      logger.warn("Error while generating a TFT rank message!", e);
      return;
    }

    TextChannel textChannelWhereSend = Zoe.getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessageEmbeds(message).queue();
    }
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
