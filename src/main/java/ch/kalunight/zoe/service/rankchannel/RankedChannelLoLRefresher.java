package ch.kalunight.zoe.service.rankchannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.GameAccessDataServerSpecific;
import ch.kalunight.zoe.model.PlayerRankedResult;
import ch.kalunight.zoe.model.RankedChangeType;
import ch.kalunight.zoe.model.dto.SavedMatch;
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

  private static final Map<GameAccessDataServerSpecific, List<LeagueAccount>> matchsToTreat = Collections.synchronizedMap(new HashedMap<GameAccessDataServerSpecific, List<LeagueAccount>>());

  private static final Map<GameAccessDataServerSpecific, List<PlayerRankedResult>> matchsWaitingToComplete = Collections.synchronizedMap(new HashedMap<GameAccessDataServerSpecific, List<PlayerRankedResult>>());

  private static final Map<GameAccessDataServerSpecific, SavedMatch> listCachedMatchs = Collections.synchronizedMap(new HashedMap<GameAccessDataServerSpecific, SavedMatch>());

  private CurrentGameInfo gameOfTheChange;

  public RankedChannelLoLRefresher(RankHistoryChannel rankChannel, LeagueEntry oldEntry, LeagueEntry newEntry,
      CurrentGameInfo gameOfTheChange, Player player, LeagueAccount leagueAccount, Server server) {
    super(rankChannel, oldEntry, newEntry, player, leagueAccount, server);
    this.gameOfTheChange = gameOfTheChange;
  }

  private TreatMultiplePlayerResponse manageMultipleAccountsInGame(RankedChangeType change) {
    GameAccessDataServerSpecific gameAccessDataServer = new GameAccessDataServerSpecific(gameOfTheChange.getGameId(), leagueAccount.leagueAccount_server, server.serv_guildId);
    List<LeagueAccount> participantsFromTheServer = matchsToTreat.get(gameAccessDataServer);

    if(participantsFromTheServer == null) {
      return new TreatMultiplePlayerResponse(TreatMultiplePlayer.SEND_ALONE, null);
    }

    synchronized (participantsFromTheServer) {
      if(participantsFromTheServer.size() >= 3) {
        PlayerRankedResult playerResult = MessageBuilderRequest.getMatchDataMutiplePlayers(oldEntry, newEntry, gameOfTheChange, leagueAccount, server.serv_language, change);

        List<PlayerRankedResult> listPlayersRankedResult = matchsWaitingToComplete.get(gameAccessDataServer);
        if(listPlayersRankedResult != null) {
          listPlayersRankedResult.add(playerResult);

          if(listPlayersRankedResult.size() == participantsFromTheServer.size()) {
            matchsWaitingToComplete.remove(gameAccessDataServer);
            matchsToTreat.remove(gameAccessDataServer);
            return new TreatMultiplePlayerResponse(TreatMultiplePlayer.SEND_MULTIPLE, MessageBuilderRequest.createCombinedMessage(listPlayersRankedResult, gameOfTheChange, server.serv_language));
          }else {
            return new TreatMultiplePlayerResponse(TreatMultiplePlayer.DO_NOTHING, null);
          }
        }else {
          listPlayersRankedResult = Collections.synchronizedList(new ArrayList<>());
          listPlayersRankedResult.add(playerResult);
          matchsWaitingToComplete.put(gameAccessDataServer, listPlayersRankedResult);
          return new TreatMultiplePlayerResponse(TreatMultiplePlayer.DO_NOTHING, null);
        }
      }
    }

    return new TreatMultiplePlayerResponse(TreatMultiplePlayer.SEND_ALONE, null);
  }

  public static void addMatchToTreat(GameAccessDataServerSpecific gameAccessData, LeagueAccount leagueAccount) {

    synchronized (gameAccessData.getPlatform()) {
      List<LeagueAccount> leagueAccounts = matchsToTreat.get(gameAccessData);

      if(leagueAccounts != null) {
        leagueAccounts.add(leagueAccount);
      }else {
        leagueAccounts = Collections.synchronizedList(new ArrayList<>());
        leagueAccounts.add(leagueAccount);
        matchsToTreat.put(gameAccessData, leagueAccounts);
        SavedMatch match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameAccessData.getGameId());
        listCachedMatchs.put(gameAccessData, match);
      }
    }
  }

  protected void sendRankChangedWithoutBO() {
    TreatMultiplePlayerResponse treatMultiplePlayerResponse = manageMultipleAccountsInGame(RankedChangeType.RANK_WITHOUT_BO);

    MessageEmbed message = null;

    if(treatMultiplePlayerResponse.getTreatChoice().equals(TreatMultiplePlayer.DO_NOTHING)) {
      return;
    }else if(treatMultiplePlayerResponse.getTreatChoice().equals(TreatMultiplePlayer.SEND_MULTIPLE)) {
      message = treatMultiplePlayerResponse.getMessageEmbed();
    }

    if(message == null) {
      message = MessageBuilderRequest.createRankChannelCardLeagueChange
          (oldEntry, newEntry, gameOfTheChange, player, leagueAccount, server.serv_language);
    }

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }

  protected void sendBOEnded() {
    TreatMultiplePlayerResponse treatMultiplePlayerResponse = manageMultipleAccountsInGame(RankedChangeType.BO_END);

    MessageEmbed message = null;

    if(treatMultiplePlayerResponse.getTreatChoice().equals(TreatMultiplePlayer.DO_NOTHING)) {
      return;
    }else if(treatMultiplePlayerResponse.getTreatChoice().equals(TreatMultiplePlayer.SEND_MULTIPLE)) {
      message = treatMultiplePlayerResponse.getMessageEmbed();
    }

    if(message == null) {
      message = MessageBuilderRequest.createRankChannelCardBoEnded(oldEntry, newEntry, gameOfTheChange,
          player, leagueAccount, server.serv_language);
    }

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }

  protected void sendBOStarted() {
    TreatMultiplePlayerResponse treatMultiplePlayerResponse = manageMultipleAccountsInGame(RankedChangeType.BO_START);

    MessageEmbed message = null;

    if(treatMultiplePlayerResponse.getTreatChoice().equals(TreatMultiplePlayer.DO_NOTHING)) {
      return;
    }else if(treatMultiplePlayerResponse.getTreatChoice().equals(TreatMultiplePlayer.SEND_MULTIPLE)) {
      message = treatMultiplePlayerResponse.getMessageEmbed();
    }

    if(message == null) {
      message = MessageBuilderRequest.createRankChannelCardBoStarted(newEntry, gameOfTheChange, player, leagueAccount, 
          server.serv_language);
    }

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }

  protected void sendBOInProgess() {
    TreatMultiplePlayerResponse treatMultiplePlayerResponse = manageMultipleAccountsInGame(RankedChangeType.BO_CHANGE);

    MessageEmbed message = null;

    if(treatMultiplePlayerResponse.getTreatChoice().equals(TreatMultiplePlayer.DO_NOTHING)) {
      return;
    }else if(treatMultiplePlayerResponse.getTreatChoice().equals(TreatMultiplePlayer.SEND_MULTIPLE)) {
      message = treatMultiplePlayerResponse.getMessageEmbed();
    }

    if(message == null) {
      message = MessageBuilderRequest.createRankChannelBoInProgress(oldEntry, newEntry,
          gameOfTheChange, player,leagueAccount, server.serv_language);
    }

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }

  }

  protected void sendLeaguePointChangeOnly() {
    TreatMultiplePlayerResponse treatMultiplePlayerResponse = manageMultipleAccountsInGame(RankedChangeType.ONLY_LP);

    MessageEmbed message = null;

    if(treatMultiplePlayerResponse.getTreatChoice().equals(TreatMultiplePlayer.DO_NOTHING)) {
      return;
    }else if(treatMultiplePlayerResponse.getTreatChoice().equals(TreatMultiplePlayer.SEND_MULTIPLE)) {
      message = treatMultiplePlayerResponse.getMessageEmbed();
    }

    if(message == null) {
      message = MessageBuilderRequest.createRankChannelCardLeaguePointChangeOnly
          (oldEntry, newEntry, gameOfTheChange, player, leagueAccount, server.serv_language);
    }

    TextChannel textChannelWhereSend = Zoe.getJda().getTextChannelById(rankChannel.rhChannel_channelId);
    if(textChannelWhereSend != null) {
      textChannelWhereSend.sendMessage(message).queue();
    }
  }
}