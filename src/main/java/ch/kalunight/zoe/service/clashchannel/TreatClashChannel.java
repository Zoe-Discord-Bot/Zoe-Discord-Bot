package ch.kalunight.zoe.service.clashchannel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.print.attribute.standard.MediaSize.Other;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.ClashStatus;
import ch.kalunight.zoe.model.dto.ClashTeamData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.CurrentGameInfo;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.repositories.CurrentGameInfoRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeam;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeamMember;
import net.rithms.riot.api.endpoints.clash.dto.ClashTournament;
import net.rithms.riot.api.endpoints.clash.dto.ClashTournamentPhase;
import net.rithms.riot.constant.Platform;

public class TreatClashChannel implements Runnable {

  private static final int CLASH_QUEUE_ID = 700;

  private static final Logger logger = LoggerFactory.getLogger(TreatClashChannel.class);

  private DTO.Server server;

  private DTO.ClashChannel clashChannelDB;

  private Guild guild;

  private TextChannel clashChannel;

  public TreatClashChannel(Server server, ClashChannel clashChannel) {
    this.server = server;
    this.clashChannelDB = clashChannel;
  }

  private class ClashTeamRegistration {
    public ClashTournament tournament;
    public ClashTeam team;

    public ClashTeamRegistration(ClashTournament tournament, ClashTeam team) {
      this.tournament = tournament;
      this.team = team;
    }
  }

  @Override
  public void run() {
    try {

      boolean loadNeedToBeCanceled = loadDiscordEntities();
      if(loadNeedToBeCanceled) {
        return;
      }

      ClashTeamData clashMessageManager = clashChannelDB.clashChannel_data;

      cleanClashChannel(clashMessageManager);

      DTO.LeagueAccount leagueAccount = LeagueAccountRepository.getLeagueAccountWithLeagueAccountId(clashMessageManager.getSelectedLeagueAccountId());

      if(leagueAccount == null) {
        clashChannel.sendMessage(LanguageManager.getText(server.serv_language, "clashChannelDeletionBecauseOfLeagueAccountDeletion")).queue();
        ClashChannelRepository.deleteClashChannel(clashChannelDB.clashChannel_id);
        return;
      }
      
      List<ClashTeamMember> clashPlayerRegistrations = Zoe.getRiotApi().getClashPlayerBySummonerIdWithRateLimit(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);

      ClashTeamRegistration firstClashTeam = getFirstRegistration(leagueAccount.leagueAccount_server, clashPlayerRegistrations);

      DTO.CurrentGameInfo currentGameInfo = CurrentGameInfoRepository.getCurrentGameWithLeagueAccountID(clashChannelDB.clashChannel_data.getSelectedLeagueAccountId());

      updateClashStatus(clashMessageManager, firstClashTeam, currentGameInfo);

      switch (clashMessageManager.getClashStatus()) {
      case WAIT_FOR_GAME_END:
        break;
      case WAIT_FOR_GAME_START:
        break;
      case WAIT_FOR_TEAM_REGISTRATION:
        refreshWaitForTeamRegistration(clashMessageManager, leagueAccount);
        break;
      }

    }catch(Exception e) {
      logger.error("Error while loading the clash channel");
    }
  }

  private void refreshWaitForTeamRegistration(ClashTeamData clashMessageManager, LeagueAccount leagueAccount) {

    cleanTeamAndGameMessages(clashMessageManager);

    StringBuilder messageBuilder = new StringBuilder();
    
    messageBuilder.append("**" + String.format(LanguageManager.getText(server.serv_language, "clashChannelTitle"), leagueAccount.leagueAccount_name,
        leagueAccount.leagueAccount_server.getName().toUpperCase()) + "**\n\n");
    
    messageBuilder.append(LanguageManager.getText(server.serv_language, "clashChannelLeagueAccountNotInGame") + "\n\n");
    
    messageBuilder.append("**" + LanguageManager.getText(server.serv_language, "clashChannelBottomNotInClashTeamMesssage") + "**");
    
    editOrCreateTheseMessages(clashMessageManager.getInfoMessagesId(), messageBuilder.toString());
  }

  private void editOrCreateTheseMessages(List<Long> infoMessagesId, String messageToSend) {
    
    List<Message> messageToEditOrDelete = new ArrayList<>();
    
    for(Long messageId : infoMessagesId) {
      try {
        messageToEditOrDelete.add(clashChannel.retrieveMessageById(messageId).complete());
      }catch(ErrorResponseException e) {
        if(!e.getErrorResponse().equals(ErrorResponse.UNKNOWN_MESSAGE)) {
          throw e;
        }
      }
    }
    
    CommandEvent.splitMessage(messageToSend); // Implement the management of the message
  }

  private void cleanClashChannel(ClashTeamData clashMessageManager) {
    List<Long> allMessagesToBeSaved = new ArrayList<>();

    allMessagesToBeSaved.addAll(clashMessageManager.getInfoMessagesId());
    allMessagesToBeSaved.addAll(clashMessageManager.getEnemyTeamMessages());
    allMessagesToBeSaved.add(clashMessageManager.getGameCardId());

    List<Message> messagesToDelete = clashChannel.getIterableHistory().stream()
        .limit(1000)
        .filter(m-> !allMessagesToBeSaved.contains(m.getIdLong()))
        .collect(Collectors.toList());

    if(messagesToDelete.size() > 1) {
      clashChannel.purgeMessages(messagesToDelete);
    }else {
      for(Message messageToDelete : messagesToDelete) {
        messageToDelete.delete().queue();
      }
    }
  }

  private boolean loadDiscordEntities() throws SQLException {
    guild = Zoe.getJda().getGuildById(server.serv_guildId);

    if(guild == null) {
      return true;
    }

    clashChannel = guild.getTextChannelById(clashChannelDB.clashChannel_channelId);
    if(clashChannel == null) {
      ClashChannelRepository.deleteClashChannel(clashChannelDB.clashChannel_id);
      return true;
    }
    return false;
  }

  private void cleanTeamAndGameMessages(ClashTeamData clashMessageManager) {

    for(Long teamMessageId : clashMessageManager.getEnemyTeamMessages()) {
      try {
        clashChannel.retrieveMessageById(teamMessageId).queue(e -> e.delete().queue());
      }catch(ErrorResponseException e) {
        if(!e.getErrorResponse().equals(ErrorResponse.UNKNOWN_MESSAGE)) {
          throw e;
        }
      }
    }

    clashMessageManager.getEnemyTeamMessages().clear();

    try {
      clashChannel.retrieveMessageById(clashMessageManager.getGameCardId()).queue(e -> e.delete().queue());
    }catch(ErrorResponseException e) {
      if(!e.getErrorResponse().equals(ErrorResponse.UNKNOWN_MESSAGE)) {
        throw e;
      }
    }

    clashMessageManager.setGameCardId(null);

  }

  private void updateClashStatus(ClashTeamData clashMessageManager, ClashTeamRegistration clashTeamRegistration, DTO.CurrentGameInfo currentGame) {

    if(clashTeamRegistration == null) {
      clashMessageManager.setClashStatus(ClashStatus.WAIT_FOR_TEAM_REGISTRATION);
    }else if(currentGame != null && currentGame.currentgame_currentgame.getGameQueueConfigId() == CLASH_QUEUE_ID) {
      clashMessageManager.setClashStatus(ClashStatus.WAIT_FOR_GAME_END);
    }else {
      clashMessageManager.setClashStatus(ClashStatus.WAIT_FOR_GAME_START);
    }

  }

  private ClashTeamRegistration getFirstRegistration(Platform platform, List<ClashTeamMember> clashPlayerRegistrations) throws RiotApiException {

    ClashTeamRegistration teamRegistration = null;

    for(ClashTeamMember clashPlayer : clashPlayerRegistrations) {
      ClashTeam team = Zoe.getRiotApi().getClashTeamByTeamIdWithRateLimit(platform, clashPlayer.getTeamId());

      ClashTournament tournamentToCheck = Zoe.getRiotApi().getClashTournamentById(platform, team.getTournamentId());

      if(teamRegistration == null || teamRegistration.tournament.getSchedule().get(0).getStartTime().isAfter(tournamentToCheck.getSchedule().get(0).getStartTime())) {
        teamRegistration = new ClashTeamRegistration(tournamentToCheck, team);
      }

    }
    return teamRegistration;
  }
}
