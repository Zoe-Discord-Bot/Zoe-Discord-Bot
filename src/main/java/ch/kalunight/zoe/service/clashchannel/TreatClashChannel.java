package ch.kalunight.zoe.service.clashchannel;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.ComparableMessage;
import ch.kalunight.zoe.model.clash.ClashTeamRegistration;
import ch.kalunight.zoe.model.clash.ClashTournamentComparator;
import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.model.dangerosityreport.PickData;
import ch.kalunight.zoe.model.dto.ClashChannelData;
import ch.kalunight.zoe.model.dto.ClashStatus;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.ClashUtil;
import ch.kalunight.zoe.util.MessageManagerUtil;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.TeamUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;
import no.stelar7.api.r4j.impl.lol.builders.summoner.SummonerBuilder;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPlayer;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPosition;
import no.stelar7.api.r4j.pojo.lol.clash.ClashRole;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTournament;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTournamentPhase;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class TreatClashChannel implements Runnable {

  private static final String BOTTOM_MESSAGE_ID = "clashChannelBottomNotInClashTeamMesssage";

  private static final DateTimeFormatter CLASH_TOURNAMENT_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private static final DateTimeFormatter CLASH_TOURNAMENT_TIME_ONLY_PATTERN = DateTimeFormatter.ofPattern("HH:mm");

  private static final Logger logger = LoggerFactory.getLogger(TreatClashChannel.class);

  private static final Comparator<ClashTournament> clashTournamentComparator = new ClashTournamentComparator();

  private DTO.Server server;

  private DTO.ClashChannel clashChannelDB;

  private TextChannel clashChannel;

  public TreatClashChannel(Server server, ClashChannel clashChannel) {
    this.server = server;
    this.clashChannelDB = clashChannel;
  }

  @Override
  public void run() {
    try {

      JDA jda = Zoe.getJdaByGuildId(server.serv_guildId);

      boolean loadNeedToBeCanceled = loadDiscordEntities(jda);
      if(loadNeedToBeCanceled) {
        return;
      }

      ClashChannelData clashMessageManager = clashChannelDB.clashChannel_data;

      cleanClashChannel(clashMessageManager);

      Summoner summonerCache = new SummonerBuilder().withPlatform(clashMessageManager.getSelectedPlatform()).withSummonerId(clashMessageManager.getSelectedSummonerId()).get();

      if(summonerCache == null) { //TODO handle timeout case -> don't delete clash channel
        //clashChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "clashChannelDeletionBecauseOfSummonerAccountUnreachable")).queue();
        //ClashChannelRepository.deleteClashChannel(clashChannelDB.clashChannel_id);
        return;
      }

      List<ClashPlayer> clashPlayerRegistrations = Zoe.getRiotApi().getLoLAPI().getClashAPI().getPlayerInfo(clashMessageManager.getSelectedPlatform(), clashMessageManager.getSelectedSummonerId());

      ClashTeamRegistration firstClashTeam = ClashUtil.getFirstRegistration(clashMessageManager.getSelectedPlatform(), clashPlayerRegistrations);

      updateClashStatus(clashMessageManager, firstClashTeam);

      switch (clashMessageManager.getClashStatus()) {
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

      clearLoadingEmote(clashMessageManager, jda);

    }catch(Exception e) {
      logger.error("Error while loading the clash channel", e);
    }
  }

  private void refreshWaitForGameStart(ClashChannelData clashMessageManager, ClashTeamRegistration firstClashTeam,
      Summoner summonerCache) {

    List<ClashPlayer> teamMembers = firstClashTeam.getTeam().getPlayers();
    List<TeamPlayerAnalysisDataCollector> teamPlayersData = TeamUtil.getTeamPlayersDataWithAnalysisDoneWithClashData(clashMessageManager.getSelectedPlatform(), teamMembers);

    StringBuilder messageBuilder = new StringBuilder();

    messageBuilder.append("**" + String.format(LanguageManager.getText(server.getLanguage(), "clashChannelTitle"), summonerCache.getName(),
        clashMessageManager.getSelectedPlatform().getRealmValue().toUpperCase()) + "**\n\n");

    addRegisteredInfo(clashMessageManager, firstClashTeam, summonerCache, messageBuilder);

    messageBuilder.append("**" + String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentTeamNameStats"), firstClashTeam.getTeam().getAbbreviation(),
        firstClashTeam.getTeam().getName(),
        firstClashTeam.getTeam().getTier()) + "**\n");

    Collections.sort(teamPlayersData);

    TeamUtil.addPlayersStats(server, teamPlayersData, messageBuilder);

    messageBuilder.append("\n");

    TeamUtil.addFlexStats(server, teamPlayersData, messageBuilder);

    messageBuilder.append("\n");

    addProbablyBan(teamPlayersData, messageBuilder);

    messageBuilder.append(String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentBottomTextFullTeam"), summonerCache.getPlatform().getRealmValue().toUpperCase()) + "\n");
    messageBuilder.append(String.format(LanguageManager.getText(server.getLanguage(), BOTTOM_MESSAGE_ID)));

    MessageManagerUtil.editOrCreateTheseMessages(clashMessageManager.getInfoMessagesId(), clashChannel, messageBuilder.toString());
  }

  private void addProbablyBan(List<TeamPlayerAnalysisDataCollector> teamPlayersData, StringBuilder messageBuilder) {
    List<PickData> dangerousPlayers = TeamUtil.getHeighestDangerosityAllTeam(teamPlayersData, 5);

    StringBuilder dangerChampionsText = new StringBuilder();

    int pickToTreat = dangerousPlayers.size();
    for(PickData dangerousData : dangerousPlayers) {
      Champion championData = Ressources.getChampionDataById(dangerousData.getChampionId());

      String championString = LanguageManager.getText(server.getLanguage(), "unknown");
      if(championData != null) {
        championString = championData.getEmoteUsable() + " " + championData.getName();
      }

      dangerChampionsText.append(championString);
      pickToTreat--;
      if(pickToTreat != 0) {
        dangerChampionsText.append(", ");
      }
    }

    messageBuilder.append(String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentPotentialBanAgainstAlly"), dangerChampionsText.toString()) + "\n");
  }

  private void refreshWaitForFullTeam(ClashChannelData clashMessageManager, ClashTeamRegistration firstClashTeam,
      Summoner summonerCache) {

    StringBuilder messageBuilder = new StringBuilder();

    messageBuilder.append("**" + String.format(LanguageManager.getText(server.getLanguage(), "clashChannelTitle"), summonerCache.getName(),
        clashMessageManager.getSelectedPlatform().getRealmValue().toUpperCase()) + "**\n\n");

    addRegisteredInfo(clashMessageManager, firstClashTeam, summonerCache, messageBuilder);


    messageBuilder.append("**" + String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentTeamName"), firstClashTeam.getTeam().getAbbreviation(),
        firstClashTeam.getTeam().getName(),
        firstClashTeam.getTeam().getTier()) + "**\n");

    //Show players
    addAllTeamToBuilder(clashMessageManager, firstClashTeam, messageBuilder);

    messageBuilder.append("\n\n" + LanguageManager.getText(server.getLanguage(), BOTTOM_MESSAGE_ID));

    MessageManagerUtil.editOrCreateTheseMessages(clashMessageManager.getInfoMessagesId(), clashChannel, messageBuilder.toString());
  }

  private void addRegisteredInfo(ClashChannelData clashMessageManager, ClashTeamRegistration firstClashTeam,
      Summoner summonerCache, StringBuilder messageBuilder) {
    String formatedSummonerName = summonerCache.getName() + " (" + clashMessageManager.getSelectedPlatform().getRealmValue().toUpperCase() + ")";

    String dayNumber = TeamUtil.parseDayId(server.getLanguage(), firstClashTeam.getTournament().getNameKeySecondary());

    String tournamentBasicName = LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentBasicName");

    String tournamentName = LanguageManager.getText(server.getLanguage(), firstClashTeam.getTournament().getNameKey());

    if(tournamentName.startsWith("Translation error")) {
      tournamentName = tournamentBasicName + " " + dayNumber;
    }else {
      tournamentName = tournamentName + " " + dayNumber;
    }

    if(firstClashTeam.getTournament().getSchedule().size() == 1) {
      ClashTournamentPhase phase = firstClashTeam.getTournament().getSchedule().get(0);
      messageBuilder.append(String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentRegistered"), formatedSummonerName,
          tournamentName, getFormatedDateFromPhase(phase), clashChannelDB.clashChannel_timezone.getID()) + "\n\n");
    }else {
      //TODO: show the correct phase according to the date for multiple phase.
    }
  }

  private void addAllTeamToBuilder(ClashChannelData clashMessageManager, ClashTeamRegistration firstClashTeam,
      StringBuilder messageBuilder) {
    addTeamMembersTextByPosition(ClashPosition.TOP, firstClashTeam.getTeam().getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    messageBuilder.append("\n");
    addTeamMembersTextByPosition(ClashPosition.JUNGLE, firstClashTeam.getTeam().getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    messageBuilder.append("\n");
    addTeamMembersTextByPosition(ClashPosition.MIDDLE, firstClashTeam.getTeam().getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    messageBuilder.append("\n");
    addTeamMembersTextByPosition(ClashPosition.BOTTOM, firstClashTeam.getTeam().getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    messageBuilder.append("\n");
    addTeamMembersTextByPosition(ClashPosition.UTILITY, firstClashTeam.getTeam().getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());

    List<ClashPlayer> membersFill = TeamUtil.getPlayerByPosition(ClashPosition.FILL, firstClashTeam.getTeam().getPlayers());
    if(!membersFill.isEmpty()) {
      messageBuilder.append("\n");
      addTeamMembersTextByPosition(ClashPosition.FILL, firstClashTeam.getTeam().getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    }

    List<ClashPlayer> membersUnselected = TeamUtil.getPlayerByPosition(ClashPosition.UNSELECTED, firstClashTeam.getTeam().getPlayers());
    if(!membersUnselected.isEmpty()) {
      messageBuilder.append("\n");
      addTeamMembersTextByPosition(ClashPosition.UNSELECTED, firstClashTeam.getTeam().getPlayers(), messageBuilder, clashMessageManager.getSelectedPlatform());
    }
  }

  private void addTeamMembersTextByPosition(ClashPosition position, List<ClashPlayer> players,
      StringBuilder messageBuilder, LeagueShard platform) {
    List<ClashPlayer> members = TeamUtil.getPlayerByPosition(position, players);

    StringBuilder playerName = new StringBuilder();

    playerName.append(LanguageManager.getText(server.getLanguage(), TeamUtil.getTeamPositionAbrID(position)) + " : ");

    if(members.isEmpty()) {
      playerName.append("*" + LanguageManager.getText(server.getLanguage(), "empty") + "*");
    }else {
      int numberOfPlayerTreated = 0;
      for(ClashPlayer player : members) {
        numberOfPlayerTreated++;
        Summoner summoner = new SummonerBuilder().withPlatform(platform).withSummonerId(player.getSummonerId()).get();

        String summonerName;
        if(player.getRole() == ClashRole.CAPTAIN) {
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

  private void refreshWaitForTeamRegistration(ClashChannelData clashMessageManager, Summoner summonerCache) {

    cleanTeamAndGameMessages(clashMessageManager);

    StringBuilder messageBuilder = new StringBuilder();

    messageBuilder.append("**" + String.format(LanguageManager.getText(server.getLanguage(), "clashChannelTitle"), summonerCache.getName(),
        clashMessageManager.getSelectedPlatform().getRealmValue().toUpperCase()) + "**\n\n");

    messageBuilder.append(LanguageManager.getText(server.getLanguage(), "clashChannelLeagueAccountNotInGame") + "\n\n");

    List<ClashTournament> nextTournaments = Zoe.getRiotApi().getLoLAPI().getClashAPI().getTournaments(clashMessageManager.getSelectedPlatform());

    if(nextTournaments == null || nextTournaments.isEmpty()) {
      messageBuilder.append(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentNotAvailable") + "\n\n");
    }else {
      messageBuilder.append(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentUpcoming") + "\n");

      String tournamentBasicName = LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentBasicName");

      nextTournaments.sort(clashTournamentComparator);

      for(ClashTournament clashTournament : nextTournaments) {
        List<ClashTournamentPhase> phases = clashTournament.getSchedule();

        if(phases.size() == 1) {
          addClashTournamentOnePhase(messageBuilder, tournamentBasicName, clashTournament, phases);
        }else {
          addClashTournamentMultplePhase(messageBuilder, tournamentBasicName, clashTournament, phases);
        }
      }
    }

    messageBuilder.append("\n**" + LanguageManager.getText(server.getLanguage(), BOTTOM_MESSAGE_ID) + "**");

    MessageManagerUtil.editOrCreateTheseMessages(clashMessageManager.getInfoMessagesId(), clashChannel, messageBuilder.toString());
  }

  private void addClashTournamentMultplePhase(StringBuilder messageBuilder, String tournamentBasicName,
      ClashTournament clashTournament, List<ClashTournamentPhase> phases) {
    String tournamentName = LanguageManager.getText(server.getLanguage(), clashTournament.getNameKey());

    if(tournamentName.startsWith("Translation error")) {
      tournamentName = tournamentBasicName;
    }

    messageBuilder.append(String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentMultiplePhaseTitle"), tournamentName) + "\n");

    int phaseNumber = 0;
    for(ClashTournamentPhase phase : phases) {
      phaseNumber++;
      String currentPhase = String.format(LanguageManager.getText(server.getLanguage(), "phaseNumber"), phaseNumber);
      messageBuilder.append(String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentPhaseElement"),
          currentPhase, getFormatedDateFromPhase(phase)));
      if(phases.size() != phaseNumber) {
        messageBuilder.append("\n");
      }
    }
  }

  private void addClashTournamentOnePhase(StringBuilder messageBuilder, String tournamentBasicName,
      ClashTournament clashTournament, List<ClashTournamentPhase> phases) {
    ClashTournamentPhase phase = phases.get(0);

    String dayNumber = TeamUtil.parseDayId(server.getLanguage(), clashTournament.getNameKeySecondary());

    String tournamentName = LanguageManager.getText(server.getLanguage(), clashTournament.getNameKey());

    if(tournamentName.startsWith("Translation error")) {
      tournamentName = tournamentBasicName + " " + dayNumber;
    }else {
      tournamentName = tournamentName + " " + dayNumber;
    }

    String formatedDate = getFormatedDateFromPhase(phase);

    messageBuilder.append(String.format(LanguageManager.getText(server.getLanguage(), "clashChannelClashTournamentUpcomingInfo"), tournamentName, 
        formatedDate, clashChannelDB.clashChannel_timezone.getID()) + "\n");
  }

  private String getFormatedDateFromPhase(ClashTournamentPhase phase) {
    ZonedDateTime dateTimeRegistrationToShow = phase.getRegistrationTimeAsDate().withZoneSameInstant(ZoneId.of(clashChannelDB.clashChannel_timezone.getID()));
    ZonedDateTime dateTimeStartToShow = phase.getStartTimeAsDate().withZoneSameInstant(ZoneId.of(clashChannelDB.clashChannel_timezone.getID()));

    return CLASH_TOURNAMENT_DATE_TIME_PATTERN.format(dateTimeRegistrationToShow) + "-" + CLASH_TOURNAMENT_TIME_ONLY_PATTERN.format(dateTimeStartToShow);
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

  private void clearLoadingEmote(ClashChannelData clashMessageManager, JDA jda) {
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
            messageReaction.removeReaction(jda.getSelfUser()).queue();
          } catch (ErrorResponseException e) {
            if(e.getErrorResponse() != ErrorResponse.MISSING_PERMISSIONS && e.getErrorResponse() != ErrorResponse.UNKNOWN_MESSAGE) {
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
        if(e.getErrorResponse() != ErrorResponse.MISSING_PERMISSIONS && e.getErrorResponse() != ErrorResponse.UNKNOWN_MESSAGE) {
          throw e;
        }
      }
    }
  }

  private boolean loadDiscordEntities(JDA jda) throws SQLException {
    Guild guild = jda.getGuildById(server.serv_guildId);

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
      clearDependingOfStatus(clashMessageManager.getClashStatus(), ClashStatus.WAIT_FOR_TEAM_REGISTRATION, clashMessageManager);
      clashMessageManager.setClashStatus(ClashStatus.WAIT_FOR_TEAM_REGISTRATION);
    }else if(clashTeamRegistration.getTeam().getPlayers().size() == 5) {
      clearDependingOfStatus(clashMessageManager.getClashStatus(), ClashStatus.WAIT_FOR_GAME_START, clashMessageManager);
      clashMessageManager.setClashStatus(ClashStatus.WAIT_FOR_GAME_START);
    }else {
      clearDependingOfStatus(clashMessageManager.getClashStatus(), ClashStatus.WAIT_FOR_FULL_TEAM, clashMessageManager);
      clashMessageManager.setClashStatus(ClashStatus.WAIT_FOR_FULL_TEAM);
    }

  }

  private void clearDependingOfStatus(ClashStatus oldStatus, ClashStatus newStatus, ClashChannelData clashMessageManager) {

    if(oldStatus == ClashStatus.WAIT_FOR_GAME_START && newStatus != ClashStatus.WAIT_FOR_GAME_START) {
      for(Long messageId : clashMessageManager.getEnemyTeamMessages()) {
        try {
          Message messageLoaded = clashChannel.retrieveMessageById(messageId).complete();
          messageLoaded.delete().queue();
        }catch (ErrorResponseException e) {
          logger.info("Error while deleting ennemy message", e);
        }
      }
      clashMessageManager.getEnemyTeamMessages().clear();
    }
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
