package ch.kalunight.zoe.service.clashchannel;

import java.sql.SQLException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.ClashStatus;
import ch.kalunight.zoe.model.dto.ClashChannelData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.repositories.CurrentGameInfoRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.SummonerCacheRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.ClashUtil;
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

  private boolean forceRefreshCache;

  public TreatClashChannel(Server server, ClashChannel clashChannel, boolean forceRefreshCache) {
    this.server = server;
    this.clashChannelDB = clashChannel;
    this.forceRefreshCache = forceRefreshCache;
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

      ClashChannelData clashMessageManager = clashChannelDB.clashChannel_data;

      cleanClashChannel(clashMessageManager);

      DTO.LeagueAccount leagueAccount = LeagueAccountRepository.getLeagueAccountWithSummonerId(server.serv_guildId, clashMessageManager.getSelectedSummonerId(),
          clashMessageManager.getSelectedPlatform());

      SavedSummoner summonerCache = Zoe.getRiotApi().getSummoner(clashMessageManager.getSelectedPlatform(), clashMessageManager.getSelectedSummonerId(), forceRefreshCache);

      if(leagueAccount == null) {
        clashChannel.sendMessage(LanguageManager.getText(server.serv_language, "clashChannelDeletionBecauseOfLeagueAccountDeletion")).queue();
        ClashChannelRepository.deleteClashChannel(clashChannelDB.clashChannel_id);
        return;
      }

      List<ClashTeamMember> clashPlayerRegistrations = Zoe.getRiotApi().getClashPlayerBySummonerIdWithRateLimit(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);

      ClashTeamRegistration firstClashTeam = getFirstRegistration(leagueAccount.leagueAccount_server, clashPlayerRegistrations);

      updateClashStatus(clashMessageManager, firstClashTeam);

      switch (clashMessageManager.getClashStatus()) {
      case WAIT_FOR_GAME_END:
        break;
      case WAIT_FOR_GAME_START:
        break;
      case WAIT_FOR_TEAM_REGISTRATION:
        refreshWaitForTeamRegistration(clashMessageManager, leagueAccount, summonerCache);
        break;
      }

    }catch(Exception e) {
      logger.error("Error while loading the clash channel", e);
    }
  }

  private void refreshWaitForTeamRegistration(ClashChannelData clashMessageManager, LeagueAccount leagueAccount, SavedSummoner summonerCache) {

    cleanTeamAndGameMessages(clashMessageManager);

    StringBuilder messageBuilder = new StringBuilder();

    messageBuilder.append("**" + String.format(LanguageManager.getText(server.serv_language, "clashChannelTitle"), summonerCache.getName(),
        leagueAccount.leagueAccount_server.getName().toUpperCase()) + "**\n\n");

    messageBuilder.append(LanguageManager.getText(server.serv_language, "clashChannelLeagueAccountNotInGame") + "\n\n");

    List<ClashTournament> nextTournaments = null;
    try {
      nextTournaments = Zoe.getRiotApi().getClashTournamentsWithRateLimit(leagueAccount.leagueAccount_server, forceRefreshCache);

      if(nextTournaments == null || nextTournaments.isEmpty()) {
        messageBuilder.append(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentNotAvailable") + "\n\n");
      }else {
        messageBuilder.append(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentUpcoming") + "\n");

        String tournamentBasicName = LanguageManager.getText(server.serv_language, "clashChannelClashTournamentBasicName");

        for(ClashTournament clashTournament : nextTournaments) {
          List<ClashTournamentPhase> phases = clashTournament.getSchedule();

          if(phases.size() == 1) {

            ClashTournamentPhase phase = phases.get(0);

            String dayNumber = ClashUtil.parseDayId(server.serv_language, clashTournament.getNameKeySecondary());

            String tournamentName = LanguageManager.getText(server.serv_language, clashTournament.getNameKey());

            if(tournamentName.startsWith("Translation error")) {
              tournamentName = tournamentBasicName + " " + dayNumber;
            }else {
              tournamentName = tournamentName + " " + dayNumber;
            }

            messageBuilder.append(String.format(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentUpcomingInfo"), tournamentName, 
                phase.getStartTime().withZoneSameInstant(ZoneId.of(clashChannelDB.clashChannel_timezone.getID())), clashChannelDB.clashChannel_timezone.getDisplayName())); //Suite implementation timeZone
          }
        }
      }

    } catch (SQLException e) {
      logger.warn("SQL Error in refreshWaitForTeamRegistration !", e);

      messageBuilder.append(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentError") + "\n\n");
    }

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
          logger.error("Unexpected error when getting a message", e);
          throw e;
        }
      }
    }

    List<String> messagesToSendCutted = CommandEvent.splitMessage(messageToSend); 

    if(messageToEditOrDelete.size() > messagesToSendCutted.size()) {
      int messagesToTreat = messagesToSendCutted.size();
      int messageToGet = 0;

      for(Message messageToTreat : messageToEditOrDelete) {
        if(messagesToTreat != 0) {
          messageToTreat.editMessage(messagesToSendCutted.get(messageToGet)).queue();
          messageToGet++;
          messagesToTreat++;
        }else {
          messageToTreat.delete().queue();
        }
      }
    }else if (messageToEditOrDelete.size() == messagesToSendCutted.size()) {
      int messageToGet = 0;
      for(Message messageToTreat : messageToEditOrDelete) {
        messageToTreat.editMessage(messagesToSendCutted.get(messageToGet)).queue();
        messageToGet++;
      }
    }else {
      int messagesAlreadyCreated = messageToEditOrDelete.size();
      int messagesAlreadyTreated = 0;

      for(String messageToEditOrCreate : messagesToSendCutted) {
        if(messagesAlreadyTreated < messagesAlreadyCreated) {
          messageToEditOrDelete.get(messagesAlreadyTreated).editMessage(messageToEditOrCreate).queue();
          messagesAlreadyTreated++;
        }else {
          clashChannel.sendMessage(messageToEditOrCreate).queue();
        }
      }
    }
    // Implement the management of the message (maybe with time order management ?)
  }

  private void cleanClashChannel(ClashChannelData clashMessageManager) {
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

  private void cleanTeamAndGameMessages(ClashChannelData clashMessageManager) {

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

  private void updateClashStatus(ClashChannelData clashMessageManager, ClashTeamRegistration clashTeamRegistration) {

    if(clashTeamRegistration == null) {
      clashMessageManager.setClashStatus(ClashStatus.WAIT_FOR_TEAM_REGISTRATION);
    }else if(clashMessageManager.getGameCardId() != null) {
      clashMessageManager.setClashStatus(ClashStatus.WAIT_FOR_GAME_END);
    }else {
      clashMessageManager.setClashStatus(ClashStatus.WAIT_FOR_GAME_START);
    }

  }

  private ClashTeamRegistration getFirstRegistration(Platform platform, List<ClashTeamMember> clashPlayerRegistrations) throws RiotApiException, SQLException {

    ClashTeamRegistration teamRegistration = null;

    for(ClashTeamMember clashPlayer : clashPlayerRegistrations) {
      ClashTeam team = Zoe.getRiotApi().getClashTeamByTeamIdWithRateLimit(platform, clashPlayer.getTeamId());

      ClashTournament tournamentToCheck = Zoe.getRiotApi().getClashTournamentById(platform, team.getTournamentIdInt(), forceRefreshCache);

      if(teamRegistration == null || teamRegistration.tournament.getSchedule().get(0).getStartTime().isAfter(tournamentToCheck.getSchedule().get(0).getStartTime())) {
        teamRegistration = new ClashTeamRegistration(tournamentToCheck, team);
      }

    }
    return teamRegistration;
  }
}
