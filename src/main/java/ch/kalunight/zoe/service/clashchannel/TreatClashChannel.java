package ch.kalunight.zoe.service.clashchannel;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.ClashStatus;
import ch.kalunight.zoe.model.ComparableMessage;
import ch.kalunight.zoe.model.clash.DataPerChampion;
import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReport;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportFlexPick;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportHighMastery;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportHighWinrate;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportType;
import ch.kalunight.zoe.model.dangerosityreport.PickData;
import ch.kalunight.zoe.model.dto.ClashChannelData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.InfoChannel;
import ch.kalunight.zoe.model.dto.DTO.InfoPanelMessage;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.repositories.CurrentGameInfoRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.SummonerCacheRepository;
import ch.kalunight.zoe.service.analysis.ChampionRole;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.LanguageUtil;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.TeamUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.clash.constant.TeamPosition;
import net.rithms.riot.api.endpoints.clash.constant.TeamRole;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeam;
import net.rithms.riot.api.endpoints.clash.dto.ClashTeamMember;
import net.rithms.riot.api.endpoints.clash.dto.ClashTournament;
import net.rithms.riot.api.endpoints.clash.dto.ClashTournamentPhase;
import net.rithms.riot.constant.Platform;

public class TreatClashChannel implements Runnable {

  private static final DateTimeFormatter CLASH_TOURNAMENT_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private static final DateTimeFormatter CLASH_TOURNAMENT_TIME_ONLY_PATTERN = DateTimeFormatter.ofPattern("HH:mm");

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

      SavedSummoner summonerCache = Zoe.getRiotApi().getSummoner(clashMessageManager.getSelectedPlatform(), clashMessageManager.getSelectedSummonerId(), forceRefreshCache);

      if(summonerCache == null) {
        clashChannel.sendMessage(LanguageManager.getText(server.serv_language, "clashChannelDeletionBecauseOfSummonerAccountUnreachable")).queue();
        ClashChannelRepository.deleteClashChannel(clashChannelDB.clashChannel_id);
        return;
      }

      List<ClashTeamMember> clashPlayerRegistrations = Zoe.getRiotApi().getClashPlayerBySummonerIdWithRateLimit(clashMessageManager.getSelectedPlatform(),
          clashMessageManager.getSelectedSummonerId());

      ClashTeamRegistration firstClashTeam = getFirstRegistration(clashMessageManager.getSelectedPlatform(), clashPlayerRegistrations);

      updateClashStatus(clashMessageManager, firstClashTeam);

      switch (clashMessageManager.getClashStatus()) {
      case WAIT_FOR_GAME_END:
        break;
      case WAIT_FOR_TEAM_REGISTRATION:
        refreshWaitForTeamRegistration(clashMessageManager, summonerCache);
        break;
      case WAIT_FOR_FULL_TEAM:
        refreshWaitForFullTeam(clashMessageManager, firstClashTeam, summonerCache);
        break;
      case WAIT_FOR_GAME_START:
        refreshWaitForGameStart(clashMessageManager, firstClashTeam, summonerCache);
        break;
      }

      checkMessageDisplaySync(clashMessageManager.getAllClashChannel(), clashChannel);

      ClashChannelRepository.updateClashChannel(clashMessageManager, clashChannelDB.clashChannel_id);

      clearLoadingEmote(clashMessageManager);
      
    }catch(Exception e) {
      logger.error("Error while loading the clash channel", e);
    }
  }

  private void refreshWaitForGameStart(ClashChannelData clashMessageManager, ClashTeamRegistration firstClashTeam,
      SavedSummoner summonerCache) {

    List<ClashTeamMember> teamMembers = firstClashTeam.team.getPlayers();
    List<TeamPlayerAnalysisDataCollector> teamPlayersData = TeamUtil.getTeamPlayersData(clashMessageManager, teamMembers);

    StringBuilder messageBuilder = new StringBuilder();

    messageBuilder.append("**" + String.format(LanguageManager.getText(server.serv_language, "clashChannelTitle"), summonerCache.getName(),
        clashMessageManager.getSelectedPlatform().getName().toUpperCase()) + "**\n\n");

    addRegisteredInfo(clashMessageManager, firstClashTeam, summonerCache, messageBuilder);

    messageBuilder.append("**" + String.format(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentTeamNameStats"), firstClashTeam.team.getAbbreviation(),
        firstClashTeam.team.getName(),
        firstClashTeam.team.getTier().getTier()) + "**\n");

    Collections.sort(teamPlayersData);

    for(TeamPlayerAnalysisDataCollector playerToShow : teamPlayersData) {

      String translationRole = LanguageManager.getText(server.serv_language, TeamUtil.getChampionRoleAbrID(playerToShow.getFinalDeterminedPosition()));

      String elo = LanguageManager.getText(server.serv_language, "unranked");

      FullTier rank = playerToShow.getHeighestRank();

      if(rank != null) {
        elo = playerToShow.getHeighestRankType(server.serv_language) + " : " + rank.toString(server.serv_language);
      }

      messageBuilder.append("**" + String.format(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentPlayerData"), translationRole,
          playerToShow.getSummoner().getName(), elo) + "**");

      messageBuilder.append("\n");

      List<DataPerChampion> champions = playerToShow.getMostPlayedChampions(3);

      int championToLoad = champions.size();
      for(DataPerChampion champion : champions) {
        messageBuilder.append("  -> ");

        Champion championData = Ressources.getChampionDataById(champion.getChampionId());

        String championString = LanguageManager.getText(server.serv_language, "unknown");
        if(championData != null) {
          championString = championData.getEmoteUsable() + " " + championData.getName();
        }

        String winrate = LanguageManager.getText(server.serv_language, "unknown");
        int nbrGames = 0;
        String masteryPoint = "0";

        for(DangerosityReport report : champion.getDangerosityReports()) {
          if(report instanceof DangerosityReportHighWinrate) {
            DangerosityReportHighWinrate winrateReport = (DangerosityReportHighWinrate) report;
            winrate = DangerosityReport.POURCENTAGE_FORMAT.format(winrateReport.getWinrate());
            nbrGames = winrateReport.getNbrGames();
          }

          if(report instanceof DangerosityReportHighMastery) {
            DangerosityReportHighMastery masteryReport = (DangerosityReportHighMastery) report;
            masteryPoint = LanguageUtil.convertMasteryToReadableText(masteryReport.getRawMastery());
          } 
        }

        messageBuilder.append(String.format(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentPlayerDataChampion"),
            championString, Integer.toString(nbrGames), winrate + "%", masteryPoint));
        championToLoad--;
        if(championToLoad != 0) {
          messageBuilder.append("\n");
        }
      }

      messageBuilder.append("\n\n"); 
    }

    List<DataPerChampion> championsFlex = TeamUtil.getFlexPickMostPlayed(teamPlayersData, 3);

    StringBuilder flexChampionsText = new StringBuilder();

    int championToTreat;

    if(championsFlex.size() < 3) {
      championToTreat = championsFlex.size();
    }else {
      championToTreat = 3;
    }

    for(DataPerChampion flexPick : championsFlex) {
      Champion championData = Ressources.getChampionDataById(flexPick.getChampionId());

      String championString = LanguageManager.getText(server.serv_language, "unknown");
      if(championData != null) {
        championString = championData.getEmoteUsable() + " " + championData.getName();
      }

      flexChampionsText.append(championString + " ("); 

      DangerosityReportFlexPick flexReport = (DangerosityReportFlexPick) flexPick.getDangerosityReport(DangerosityReportType.FLEX_PICK);
      int roleToTreat = flexReport.getRolesWherePlayed().size();
      for(ChampionRole role : flexReport.getRolesWherePlayed()) {

        flexChampionsText.append(LanguageManager.getText(server.serv_language, TeamUtil.getChampionRoleAbrID(role)));

        roleToTreat--;
        if(roleToTreat != 0) {
          flexChampionsText.append(", ");
        }else {
          flexChampionsText.append(")");
        }
      }

      championToTreat--;
      if(championToTreat != 0) {
        flexChampionsText.append(", ");
      }else {
        break;
      }
    }

    messageBuilder.append(String.format(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentPotentialFlexPick"), flexChampionsText.toString()) + "\n");

    List<PickData> dangerousPlayers = TeamUtil.getHeighestDangerosity(teamPlayersData, 3);

    StringBuilder dangerChampionsText = new StringBuilder();

    int pickToTreat = dangerousPlayers.size();
    for(PickData dangerousData : dangerousPlayers) {
      Champion championData = Ressources.getChampionDataById(dangerousData.getChampionId());

      String championString = LanguageManager.getText(server.serv_language, "unknown");
      if(championData != null) {
        championString = championData.getEmoteUsable() + " " + championData.getName();
      }

      dangerChampionsText.append(championString);
      pickToTreat--;
      if(pickToTreat != 0) {
        dangerChampionsText.append(", ");
      }
    }

    messageBuilder.append(String.format(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentPotentialBamAgainstAlly"), dangerChampionsText.toString()) + "\n");

    editOrCreateTheseMessages(clashMessageManager.getInfoMessagesId(), messageBuilder.toString());
  }

  private void refreshWaitForFullTeam(ClashChannelData clashMessageManager, ClashTeamRegistration firstClashTeam,
      SavedSummoner summonerCache) throws RiotApiException {

    StringBuilder messageBuilder = new StringBuilder();

    messageBuilder.append("**" + String.format(LanguageManager.getText(server.serv_language, "clashChannelTitle"), summonerCache.getName(),
        clashMessageManager.getSelectedPlatform().getName().toUpperCase()) + "**\n\n");

    addRegisteredInfo(clashMessageManager, firstClashTeam, summonerCache, messageBuilder);


    messageBuilder.append("**" + String.format(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentTeamName"), firstClashTeam.team.getAbbreviation(),
        firstClashTeam.team.getName(),
        firstClashTeam.team.getTier().getTier()) + "**\n");

    //Show players
    addAllTeamToBuilder(clashMessageManager, firstClashTeam, messageBuilder);

    messageBuilder.append("\n\n" + LanguageManager.getText(server.serv_language, "clashChannelBottomNotInClashTeamMesssage"));

    editOrCreateTheseMessages(clashMessageManager.getInfoMessagesId(), messageBuilder.toString());
  }

  private void addRegisteredInfo(ClashChannelData clashMessageManager, ClashTeamRegistration firstClashTeam,
      SavedSummoner summonerCache, StringBuilder messageBuilder) {
    String formatedSummonerName = summonerCache.getName() + " (" + clashMessageManager.getSelectedPlatform().getName().toUpperCase() + ")";

    String dayNumber = TeamUtil.parseDayId(server.serv_language, firstClashTeam.tournament.getNameKeySecondary());

    String tournamentBasicName = LanguageManager.getText(server.serv_language, "clashChannelClashTournamentBasicName");

    String tournamentName = LanguageManager.getText(server.serv_language, firstClashTeam.tournament.getNameKey());

    if(tournamentName.startsWith("Translation error")) {
      tournamentName = tournamentBasicName + " " + dayNumber;
    }else {
      tournamentName = tournamentName + " " + dayNumber;
    }

    if(firstClashTeam.tournament.getSchedule().size() == 1) {
      ClashTournamentPhase phase = firstClashTeam.tournament.getSchedule().get(0);
      messageBuilder.append(String.format(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentRegistered"), formatedSummonerName,
          tournamentName, getFormatedDateFromPhase(phase), clashChannelDB.clashChannel_timezone.getID()) + "\n\n");
    }else {
      //TODO: show the correct phase according to the date for multiple phase.
    }
  }

  private void addAllTeamToBuilder(ClashChannelData clashMessageManager, ClashTeamRegistration firstClashTeam,
      StringBuilder messageBuilder) throws RiotApiException {
    addTeamMembersTextByPosition(TeamPosition.TOP, firstClashTeam.team.getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    messageBuilder.append("\n");
    addTeamMembersTextByPosition(TeamPosition.JUNGLE, firstClashTeam.team.getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    messageBuilder.append("\n");
    addTeamMembersTextByPosition(TeamPosition.MIDDLE, firstClashTeam.team.getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    messageBuilder.append("\n");
    addTeamMembersTextByPosition(TeamPosition.BOTTOM, firstClashTeam.team.getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    messageBuilder.append("\n");
    addTeamMembersTextByPosition(TeamPosition.UTILITY, firstClashTeam.team.getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());

    List<ClashTeamMember> membersFill = TeamUtil.getPlayerByPosition(TeamPosition.FILL, firstClashTeam.team.getPlayers());
    if(!membersFill.isEmpty()) {
      messageBuilder.append("\n");
      addTeamMembersTextByPosition(TeamPosition.FILL, firstClashTeam.team.getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    }

    List<ClashTeamMember> membersUnselected = TeamUtil.getPlayerByPosition(TeamPosition.UNSELECTED, firstClashTeam.team.getPlayers());
    if(!membersUnselected.isEmpty()) {
      messageBuilder.append("\n");
      addTeamMembersTextByPosition(TeamPosition.UNSELECTED, firstClashTeam.team.getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    }
  }

  private void addTeamMembersTextByPosition(TeamPosition position, List<ClashTeamMember> players,
      StringBuilder messageBuilder, Platform platform) throws RiotApiException {
    List<ClashTeamMember> members = TeamUtil.getPlayerByPosition(position, players);

    StringBuilder playerName = new StringBuilder();

    playerName.append(LanguageManager.getText(server.serv_language, TeamUtil.getTeamPositionAbrID(position)) + " : ");

    if(members.isEmpty()) {
      playerName.append("*" + LanguageManager.getText(server.serv_language, "empty") + "*");
    }else {
      int numberOfPlayerTreated = 0;
      for(ClashTeamMember player : members) {
        numberOfPlayerTreated++;
        SavedSummoner summoner = Zoe.getRiotApi().getSummonerWithRateLimit(platform, player.getSummonerId(), forceRefreshCache);

        String summonerName;
        if(player.getTeamRole() == TeamRole.CAPTAIN) {
          summonerName = "👑 " + summoner.getName();
        }else {
          summonerName = summoner.getName();
        }

        if(numberOfPlayerTreated == 1) {
          playerName.append(summonerName);
        }else {
          playerName.append(" " + summonerName);
        }

        if(numberOfPlayerTreated != members.size()) {
          playerName.append(",");
        }
      }
    }

    messageBuilder.append(playerName.toString());

  }

  private void refreshWaitForTeamRegistration(ClashChannelData clashMessageManager, SavedSummoner summonerCache) {

    cleanTeamAndGameMessages(clashMessageManager);

    StringBuilder messageBuilder = new StringBuilder();

    messageBuilder.append("**" + String.format(LanguageManager.getText(server.serv_language, "clashChannelTitle"), summonerCache.getName(),
        clashMessageManager.getSelectedPlatform().getName().toUpperCase()) + "**\n\n");

    messageBuilder.append(LanguageManager.getText(server.serv_language, "clashChannelLeagueAccountNotInGame") + "\n\n");

    List<ClashTournament> nextTournaments = null;
    try {
      nextTournaments = Zoe.getRiotApi().getClashTournamentsWithRateLimit(clashMessageManager.getSelectedPlatform());

      if(nextTournaments == null || nextTournaments.isEmpty()) {
        messageBuilder.append(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentNotAvailable") + "\n\n");
      }else {
        messageBuilder.append(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentUpcoming") + "\n");

        String tournamentBasicName = LanguageManager.getText(server.serv_language, "clashChannelClashTournamentBasicName");

        for(ClashTournament clashTournament : nextTournaments) {
          List<ClashTournamentPhase> phases = clashTournament.getSchedule();

          if(phases.size() == 1) {
            addClashTournamentOnePhase(messageBuilder, tournamentBasicName, clashTournament, phases);
          }else {
            addClashTournamentMultplePhase(messageBuilder, tournamentBasicName, clashTournament, phases);
          }
        }
      }

    } catch (SQLException e) {
      logger.warn("SQL Error in refreshWaitForTeamRegistration !", e);

      messageBuilder.append("\n" + LanguageManager.getText(server.serv_language, "clashChannelClashTournamentError") + "\n\n");
    }

    messageBuilder.append("\n**" + LanguageManager.getText(server.serv_language, "clashChannelBottomNotInClashTeamMesssage") + "**");

    editOrCreateTheseMessages(clashMessageManager.getInfoMessagesId(), messageBuilder.toString());
  }

  private void addClashTournamentMultplePhase(StringBuilder messageBuilder, String tournamentBasicName,
      ClashTournament clashTournament, List<ClashTournamentPhase> phases) {
    String tournamentName = LanguageManager.getText(server.serv_language, clashTournament.getNameKey());

    if(tournamentName.startsWith("Translation error")) {
      tournamentName = tournamentBasicName;
    }

    messageBuilder.append(String.format(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentMultiplePhaseTitle"), tournamentName) + "\n");

    int phaseNumber = 0;
    for(ClashTournamentPhase phase : phases) {
      phaseNumber++;
      String currentPhase = String.format(LanguageManager.getText(server.serv_language, "phaseNumber"), phaseNumber);
      messageBuilder.append(String.format(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentPhaseElement"),
          currentPhase, getFormatedDateFromPhase(phase)));
      if(phases.size() != phaseNumber) {
        messageBuilder.append("\n");
      }
    }
  }

  private void addClashTournamentOnePhase(StringBuilder messageBuilder, String tournamentBasicName,
      ClashTournament clashTournament, List<ClashTournamentPhase> phases) {
    ClashTournamentPhase phase = phases.get(0);

    String dayNumber = TeamUtil.parseDayId(server.serv_language, clashTournament.getNameKeySecondary());

    String tournamentName = LanguageManager.getText(server.serv_language, clashTournament.getNameKey());

    if(tournamentName.startsWith("Translation error")) {
      tournamentName = tournamentBasicName + " " + dayNumber;
    }else {
      tournamentName = tournamentName + " " + dayNumber;
    }

    String formatedDate = getFormatedDateFromPhase(phase);

    messageBuilder.append(String.format(LanguageManager.getText(server.serv_language, "clashChannelClashTournamentUpcomingInfo"), tournamentName, 
        formatedDate, clashChannelDB.clashChannel_timezone.getID()) + "\n");
  }

  private String getFormatedDateFromPhase(ClashTournamentPhase phase) {
    ZonedDateTime dateTimeRegistrationToShow = phase.getRegistrationTime().withZoneSameInstant(ZoneId.of(clashChannelDB.clashChannel_timezone.getID()));
    ZonedDateTime dateTimeStartToShow = phase.getStartTime().withZoneSameInstant(ZoneId.of(clashChannelDB.clashChannel_timezone.getID()));

    return CLASH_TOURNAMENT_DATE_TIME_PATTERN.format(dateTimeRegistrationToShow) + "-" + CLASH_TOURNAMENT_TIME_ONLY_PATTERN.format(dateTimeStartToShow);
  }

  private void editOrCreateTheseMessages(List<Long> messageListToUpdate, String messageToSend) {

    List<Message> messageToEditOrDelete = new ArrayList<>();
    List<Long> messagesNotFound = new ArrayList<>();

    for(Long messageId : messageListToUpdate) {
      try {
        messageToEditOrDelete.add(clashChannel.retrieveMessageById(messageId).complete());
      }catch(ErrorResponseException e) {
        if(!e.getErrorResponse().equals(ErrorResponse.UNKNOWN_MESSAGE)) {
          logger.error("Unexpected error when getting a message", e);
          throw e;
        }else {
          messagesNotFound.add(messageId);
        }
      }
    }

    messageListToUpdate.removeAll(messagesNotFound);

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
          messageListToUpdate.remove(messageToTreat.getIdLong());
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
          messageListToUpdate.add(clashChannel.sendMessage(messageToEditOrCreate).complete().getIdLong());
        }
      }
    }
    // Implement the management of the message (maybe with time order management ?)
  }

  private void cleanClashChannel(ClashChannelData clashMessageManager) {
    List<Long> allMessagesToBeSaved = clashMessageManager.getAllClashChannel();

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

    addLoadingEmote(clashMessageManager);
  }

  private void clearLoadingEmote(ClashChannelData clashMessageManager) {
    for(Long messageToClear : clashMessageManager.getInfoMessagesId()) {

      Message retrievedMessage;
      try {
        retrievedMessage = clashChannel.retrieveMessageById(messageToClear).complete();
      } catch (ErrorResponseException e) {
        logger.warn("Error when deleting loading emote : {}", e.getMessage(), e);
        continue;
      }

      if(retrievedMessage != null) {
        for(MessageReaction messageReaction : retrievedMessage.getReactions()) {
          try {
            messageReaction.removeReaction(Zoe.getJda().getSelfUser()).queue();
          } catch (ErrorResponseException e) {
            if(e.getErrorResponse() != ErrorResponse.MISSING_PERMISSIONS) {
              logger.warn("Error when removing reaction : {}", e.getMessage(), e);
            }
          }
        }
      }
    }
  }

  private void addLoadingEmote(ClashChannelData clashMessageManager) {
    for(Long clashChannelId : clashMessageManager.getInfoMessagesId()) {
      try {
        Message message = clashChannel.retrieveMessageById(clashChannelId).complete();
        message.addReaction("U+23F3").complete();
      }catch(ErrorResponseException e) {
        if(e.getErrorResponse() != ErrorResponse.MISSING_PERMISSIONS) {
          throw e;
        }
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
      if(clashMessageManager.getGameCardId() != null) {
        clashChannel.retrieveMessageById(clashMessageManager.getGameCardId()).queue(e -> e.delete().queue());
      }
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
    }else if(clashTeamRegistration.team.getPlayers().size() == 5) {
      clashMessageManager.setClashStatus(ClashStatus.WAIT_FOR_GAME_START);
    }else {
      clashMessageManager.setClashStatus(ClashStatus.WAIT_FOR_FULL_TEAM);
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

  private void checkMessageDisplaySync(List<Long> allClashMessages, TextChannel clashChannel) {

    List<Message> messagesToCheck = new ArrayList<>();
    for(Long messageIdToLoad : allClashMessages) {
      Message message = clashChannel.retrieveMessageById(messageIdToLoad).complete();
      messagesToCheck.add(message);
    }

    boolean needToResend = false;

    List<Message> orderedMessage = orderMessagesByTime(messagesToCheck);

    for(Message messageToCompare : orderedMessage) {
      for(Message secondMessageToCompare : orderedMessage) {
        if(messageToCompare.getIdLong() != secondMessageToCompare.getIdLong()) {
          if(messageToCompare.getTimeCreated().until(secondMessageToCompare.getTimeCreated(), ChronoUnit.MINUTES) > 5) {
            needToResend = true;
          }
        }
      }
    }

    if(needToResend) {

      List<String> messagesTextToResend = new ArrayList<>();

      for(Message messageToDelete : orderedMessage) {
        messagesTextToResend.add(messageToDelete.getContentRaw());
        messageToDelete.delete().queue();
      }

      for(String messageTextToString : messagesTextToResend) {
        clashChannel.sendMessage(messageTextToString).queue();
      }
    }
  }

  private List<Message> orderMessagesByTime(List<Message> messagesToCheck) {

    List<ComparableMessage> messagesToOrder = new ArrayList<>();

    for(Message message : messagesToCheck) {
      messagesToOrder.add(new ComparableMessage(message));
    }

    Collections.sort(messagesToOrder); 

    List<Message> messagesOrdered = new ArrayList<>();
    for(ComparableMessage messageOrdered : messagesToOrder) {
      messagesOrdered.add(messageOrdered.getMessage());
    }
    return messagesOrdered;
  }
}
