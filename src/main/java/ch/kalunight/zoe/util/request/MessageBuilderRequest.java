package ch.kalunight.zoe.util.request;

import java.awt.Color;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.stats.StatsProfileCommand;
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.InfocardPlayerData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.player_data.Rank;
import ch.kalunight.zoe.model.player_data.Tier;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.model.static_data.CustomEmote;
import ch.kalunight.zoe.model.static_data.Mastery;
import ch.kalunight.zoe.service.infochannel.SummonerDataWorker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.MessageBuilderRequestUtil;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.league.dto.MiniSeries;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class MessageBuilderRequest {

  private static final String BLUE_TEAM_STRING = "blueTeam";
  private static final String RED_TEAM_STRING = "redTeam";
  private static final String MASTERIES_WR_THIS_MONTH_STRING = "masteriesWrTitleRespectSize";
  private static final String SOLO_Q_RANK_STRING = "soloqTitleRespectSize";
  private static final String RESUME_OF_THE_GAME_STRING = "resumeOfTheGame";
  private static final String GENERATED_AT_STRING = "generatedAt";

  private static final Logger logger = LoggerFactory.getLogger(MessageBuilderRequest.class);

  private MessageBuilderRequest() {}

  public static MessageEmbed createRankChannelCardBoStarted(LeagueEntry newEntry, 
      CurrentGameInfo gameOfTheChange, Player player, LeagueAccount leagueAccount, String lang) {

    Match match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameOfTheChange.getGameId());

    EmbedBuilder message = new EmbedBuilder();

    String gameType = getGameType(gameOfTheChange, lang);

    User user = player.user;
    message.setAuthor(user.getName(), null, user.getAvatarUrl());

    MiniSeries bo = newEntry.getMiniSeries();
    FullTier newFullTier = new FullTier(newEntry);

    message.setColor(Color.GREEN);
    message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelBoStartedTitle"), 
        leagueAccount.leagueAccount_name, bo.getProgress().length(),
        newFullTier.getHeigerDivision().toStringWithoutLp(lang), gameType));

    String boStatus = MessageBuilderRequestUtil.getBoStatus(bo, lang);

    message.setDescription(boStatus);

    String statsGame = MessageBuilderRequestUtil.getResumeGameStats(leagueAccount, lang, match);

    Field field = new Field(LanguageManager.getText(lang, RESUME_OF_THE_GAME_STRING), statsGame, true);

    message.addField(field);

    message.setFooter(LanguageManager.getText(lang, GENERATED_AT_STRING));
    message.setTimestamp(Instant.now());

    return message.build();
  }

  public static MessageEmbed createRankChannelBoInProgress(LeagueEntry oldEntry, LeagueEntry newEntry, 
      CurrentGameInfo gameOfTheChange, Player player, LeagueAccount leagueAccount, String lang) {

    Match match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameOfTheChange.getGameId());

    EmbedBuilder message = new EmbedBuilder();

    MiniSeries oldBo = oldEntry.getMiniSeries();
    MiniSeries newBo = newEntry.getMiniSeries();

    String gameType = getGameType(gameOfTheChange, lang);

    User user = player.user;
    message.setAuthor(user.getName(), null, user.getAvatarUrl());
    
    Participant participant = match.getParticipantBySummonerId(leagueAccount.leagueAccount_summonerId);
    String winAgain = match.getTeamByTeamId(participant.getTeamId()).getWin();

    FullTier oldFullTier = new FullTier(oldEntry);

    boolean win = winAgain.equalsIgnoreCase("Win");
    
    if(win) {
      message.setColor(Color.GREEN);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeBOProgressWinTitle"),
          leagueAccount.leagueAccount_name, oldBo.getProgress().length(),
          oldFullTier.getHeigerDivision().toStringWithoutLp(lang), gameType));
    }else if(winAgain.equalsIgnoreCase("Fail")) {
      message.setColor(Color.RED);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeBOProgressLooseTitle"),
          leagueAccount.leagueAccount_name, oldBo.getProgress().length(),
          oldFullTier.getHeigerDivision().toStringWithoutLp(lang), gameType));
    }else {
      logger.info("A game in rank channel generation message has been canceled");
      return null;
    }

    String boStatus = MessageBuilderRequestUtil.getBoStatus(newBo, lang);

    message.setDescription(boStatus);

    String statsGame = MessageBuilderRequestUtil.getResumeGameStats(leagueAccount, lang, match);

    Field field = new Field(LanguageManager.getText(lang, RESUME_OF_THE_GAME_STRING), statsGame, true);

    message.addField(field);

    message.setFooter(LanguageManager.getText(lang, GENERATED_AT_STRING));
    message.setTimestamp(Instant.now());

    return message.build();
  }

  public static MessageEmbed createRankChannelCardBoEnded(LeagueEntry oldEntry, LeagueEntry newEntry, 
      CurrentGameInfo gameOfTheChange, Player player, LeagueAccount leagueAccount, String lang) throws NoValueRankException {

    Match match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameOfTheChange.getGameId());

    EmbedBuilder message = new EmbedBuilder();

    FullTier oldFullTier = new FullTier(oldEntry);
    FullTier newFullTier = new FullTier(newEntry);

    boolean boWin;

    if(oldFullTier.getTier() != newFullTier.getTier() || oldFullTier.getRank() != newFullTier.getRank()) {
      boWin = true;
    }else {
      boWin = false;
    }

    MiniSeries bo = oldEntry.getMiniSeries();
    String gameType = getGameType(gameOfTheChange, lang);

    User user = player.user;
    message.setAuthor(user.getName(), null, user.getAvatarUrl());
    
    if(!boWin) {
      message.setColor(Color.RED);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeBOEndedLooseTitle"),
          leagueAccount.leagueAccount_name, bo.getProgress().length(), oldFullTier.getHeigerDivision().toStringWithoutLp(lang), gameType));
    }else {
      message.setColor(Color.YELLOW);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeBOEndedWinTitle"),
          leagueAccount.leagueAccount_name, bo.getProgress().length(), oldFullTier.getHeigerDivision().toStringWithoutLp(lang), gameType));
    }

    message.setDescription(oldFullTier.toString(lang) + " -> " + newFullTier.toString(lang) + "\n"
        + MessageBuilderRequestUtil.getBoStatus(bo, lang, boWin));

    String statsGame = MessageBuilderRequestUtil.getResumeGameStats(leagueAccount, lang, match);

    Field field = new Field(LanguageManager.getText(lang, RESUME_OF_THE_GAME_STRING), statsGame, true);

    message.addField(field);

    message.setFooter(LanguageManager.getText(lang, GENERATED_AT_STRING));
    message.setTimestamp(Instant.now());

    return message.build();
  }

  public static MessageEmbed createRankChannelCardLeagueChange(LeagueEntry oldEntry, LeagueEntry newEntry, 
      CurrentGameInfo gameOfTheChange, Player player, LeagueAccount leagueAccount, String lang) {

    Match match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameOfTheChange.getGameId());

    EmbedBuilder message = new EmbedBuilder();

    String gameType = getGameType(gameOfTheChange, lang);

    User user = player.user;
    message.setAuthor(user.getName(), null, user.getAvatarUrl());

    boolean divisionJump = false;
    boolean goodChange;

    FullTier oldFullTier = new FullTier(oldEntry);
    FullTier newFullTier = new FullTier(newEntry);

    int valueOfTheDivisionJump;
    try {
      valueOfTheDivisionJump = newFullTier.value() - oldFullTier.value();
    } catch (NoValueRankException e) {
      valueOfTheDivisionJump = 0;
    }

    if(valueOfTheDivisionJump < 0) {
      valueOfTheDivisionJump *= -1;
      goodChange = false;
    }else {
      goodChange = true;
    }

    if(valueOfTheDivisionJump > 100) {
      divisionJump = true;
    }

    if(goodChange) {
      message.setColor(Color.YELLOW);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeRankChangeWinDivisionSkippedTitle"),
          leagueAccount.leagueAccount_name, gameType));
    }else {
      if(divisionJump) {
        message.setColor(Color.BLACK);
        message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeLooseDivisionDecayTitle"),
            leagueAccount.leagueAccount_name, gameType));
      }else {
        message.setColor(Color.RED);
        message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeLooseDivisionTitle"),
            leagueAccount.leagueAccount_name, gameType));
      }
    }

    message.setDescription(oldFullTier.toString(lang) + " -> " + newFullTier.toString(lang));

    String statsGame = MessageBuilderRequestUtil.getResumeGameStats(leagueAccount, lang, match);

    Field field = new Field(LanguageManager.getText(lang, RESUME_OF_THE_GAME_STRING), statsGame, true);

    message.addField(field);

    message.setFooter(LanguageManager.getText(lang, GENERATED_AT_STRING));
    message.setTimestamp(Instant.now());

    return message.build();
  }

  public static MessageEmbed createRankChannelCardLeaguePointChangeOnly(LeagueEntry oldEntry, LeagueEntry newEntry, 
      CurrentGameInfo gameOfTheChange, Player player, LeagueAccount leagueAccount, String lang) {

    Match match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameOfTheChange.getGameId());

    EmbedBuilder message = new EmbedBuilder();

    String gameType = getGameType(gameOfTheChange, lang);

    User user = player.user;
    message.setAuthor(user.getName(), null, user.getAvatarUrl());
    
    int lpReceived = newEntry.getLeaguePoints() - oldEntry.getLeaguePoints();
    boolean gameWin = (newEntry.getLeaguePoints() - oldEntry.getLeaguePoints()) > 0;

    if(gameWin) {
      message.setColor(Color.GREEN);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangePointOnlyWinTitle"),
          leagueAccount.leagueAccount_name, lpReceived, gameType));
    }else {
      message.setColor(Color.RED);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangePointOnlyLooseTitle"),
          leagueAccount.leagueAccount_name, lpReceived * -1, gameType));
    }

    FullTier oldFullTier = new FullTier(oldEntry);
    FullTier newFullTier = new FullTier(newEntry);

    message.setDescription(oldFullTier.toString(lang) + " -> " + newFullTier.toString(lang));

    String statsGame = MessageBuilderRequestUtil.getResumeGameStats(leagueAccount, lang, match);

    Field field = new Field(LanguageManager.getText(lang, RESUME_OF_THE_GAME_STRING), statsGame, true);

    message.addField(field);

    message.setFooter(LanguageManager.getText(lang, GENERATED_AT_STRING));
    message.setTimestamp(Instant.now());

    return message.build();
  }

  private static String getGameType(CurrentGameInfo gameOfTheChange, String lang) {
    String gameType = "Unknown rank mode";

    if(gameOfTheChange.getGameQueueConfigId() == GameQueueConfigId.SOLOQ.getId()) {
      gameType = LanguageManager.getText(lang, "soloq");
    }else if(gameOfTheChange.getGameQueueConfigId() == GameQueueConfigId.FLEX.getId()){
      gameType = LanguageManager.getText(lang, "flex");
    }
    return gameType;
  }

  public static MessageEmbed createInfoCard(List<DTO.Player> players, CurrentGameInfo currentGameInfo,
      Platform region, DTO.Server server) throws SQLException {

    String blueTeamTranslated = LanguageManager.getText(server.serv_language, BLUE_TEAM_STRING);
    String redTeamTranslated = LanguageManager.getText(server.serv_language, RED_TEAM_STRING);
    String masteriesWRThisMonthTranslated = LanguageManager.getText(server.serv_language, MASTERIES_WR_THIS_MONTH_STRING);
    String rankTitleTranslated;
    if(currentGameInfo.getGameQueueConfigId() == GameQueueConfigId.FLEX.getId()) {
      rankTitleTranslated = LanguageManager.getText(server.serv_language, "flexTitleRespectSize");
    } else {
      rankTitleTranslated = LanguageManager.getText(server.serv_language, SOLO_Q_RANK_STRING);
    }
    
    Set<DTO.LeagueAccount> playersAccountsOfTheGame = new HashSet<>();
    for(DTO.Player player : players) {
      playersAccountsOfTheGame.addAll(
          MessageBuilderRequestUtil.getLeagueAccountsInTheGivenGame(currentGameInfo, region, player, server.serv_guildId));
    }

    EmbedBuilder message = new EmbedBuilder();

    StringBuilder title = new StringBuilder();

    MessageBuilderRequestUtil.createTitle(players, currentGameInfo, title, server.serv_language, true);

    message.setTitle(title.toString());

    int blueTeamID = 100;
    ArrayList<CurrentGameParticipant> blueTeam = new ArrayList<>();
    ArrayList<CurrentGameParticipant> redTeam = new ArrayList<>();

    MessageBuilderRequestUtil.getTeamPlayer(currentGameInfo, blueTeamID, blueTeam, redTeam);

    ArrayList<String> listIdPlayers = new ArrayList<>();

    for(DTO.LeagueAccount leagueAccount : playersAccountsOfTheGame) {
      listIdPlayers.add(leagueAccount.leagueAccount_summonerId);
    }

    List<InfocardPlayerData> playersData = new ArrayList<>();

    MessageBuilderRequestUtil.createTeamDataMultipleSummoner(blueTeam, listIdPlayers, region, server.serv_language, playersData, true, currentGameInfo.getGameQueueConfigId());
    MessageBuilderRequestUtil.createTeamDataMultipleSummoner(redTeam, listIdPlayers, region, server.serv_language, playersData, false, currentGameInfo.getGameQueueConfigId());

    SummonerDataWorker.awaitAll(playersData);

    StringBuilder blueTeamString = new StringBuilder();
    StringBuilder blueTeamRankString = new StringBuilder();
    StringBuilder blueTeamWinrateString = new StringBuilder();

    StringBuilder redTeamString = new StringBuilder();
    StringBuilder redTeamRankString = new StringBuilder();
    StringBuilder redTeamWinrateString = new StringBuilder();


    for(InfocardPlayerData playerData : playersData) {
      if(playerData.isBlueTeam()) {
        blueTeamString.append(playerData.getSummonerNameData() + "\n");
        blueTeamRankString.append(playerData.getRankData() + "\n");
        blueTeamWinrateString.append(playerData.getWinRateData() + "\n");
      }else {
        redTeamString.append(playerData.getSummonerNameData() + "\n");
        redTeamRankString.append(playerData.getRankData() + "\n");
        redTeamWinrateString.append(playerData.getWinRateData() + "\n");
      }
    }

    message.addField(blueTeamTranslated, blueTeamString.toString(), true);
    message.addField(rankTitleTranslated, blueTeamRankString.toString(), true);
    message.addField(masteriesWRThisMonthTranslated, blueTeamWinrateString.toString(), true);

    message.addField(redTeamTranslated, redTeamString.toString(), true);
    message.addField(rankTitleTranslated, redTeamRankString.toString(), true);
    message.addField(masteriesWRThisMonthTranslated, redTeamWinrateString.toString(), true);

    message.setFooter(LanguageManager.getText(server.serv_language, "infoCardsGameFooter") 
        + " : " + MessageBuilderRequestUtil.getMatchTimeFromDuration(currentGameInfo.getGameLength()), null);

    message.setColor(Color.GREEN);

    return message.build();
  }

  public static MessageEmbed createProfileMessage(DTO.Player player, DTO.LeagueAccount leagueAccount,
      List<ChampionMastery> masteries, String language, String url) throws RiotApiException {

    String latestGameTranslated = LanguageManager.getText(language, "statsProfileLatestGames");

    EmbedBuilder message = new EmbedBuilder();

    Summoner summoner = Zoe.getRiotApi().getSummoner(leagueAccount.leagueAccount_server,
        leagueAccount.leagueAccount_summonerId);

    if(player != null) {
      message.setTitle(String.format(LanguageManager.getText(language, "statsProfileTitle"),
          player.user.getName(), summoner.getName(), summoner.getSummonerLevel()));
    }else {
      message.setTitle(String.format(LanguageManager.getText(language, "statsProfileTitle"),
          leagueAccount.leagueAccount_name, summoner.getName(), summoner.getSummonerLevel()));
    }

    List<ChampionMastery> threeBestchampionMasteries = StatsProfileCommand.getBestMasteries(masteries, 3);

    StringBuilder stringBuilder = new StringBuilder();

    for(ChampionMastery championMastery : threeBestchampionMasteries) {
      Champion champion = Ressources.getChampionDataById(championMastery.getChampionId());
      stringBuilder.append(champion.getDisplayName() + " " + champion.getName() + " - **" 
          + MessageBuilderRequestUtil.getMasteryUnit(championMastery.getChampionPoints()) +"**\n");
    }

    Field field = new Field(LanguageManager.getText(language, "statsProfileTopChamp"), stringBuilder.toString(), true);
    message.addField(field);

    int nbrMastery7 = 0;
    int nbrMastery6 = 0;
    int nbrMastery5 = 0;
    long totalNbrMasteries = 0;

    for(ChampionMastery championMastery : masteries) {
      switch(championMastery.getChampionLevel()) {
        case 5: nbrMastery5++; break;
        case 6: nbrMastery6++; break;
        case 7: nbrMastery7++; break;
        default: break;
      }
      totalNbrMasteries += championMastery.getChampionPoints();
    }

    double moyennePoints = (double) totalNbrMasteries / masteries.size();

    CustomEmote masteryEmote7 = Ressources.getMasteryEmote().get(Mastery.getEnum(7));
    CustomEmote masteryEmote6 = Ressources.getMasteryEmote().get(Mastery.getEnum(6));
    CustomEmote masteryEmote5 = Ressources.getMasteryEmote().get(Mastery.getEnum(5));

    field = new Field(LanguageManager.getText(language, "statsProfileMasteryStatsRespectSize"),
        nbrMastery7 + "x" + masteryEmote7.getUsableEmote() + " "
            + nbrMastery6 + "x" + masteryEmote6.getUsableEmote() + " "
            + nbrMastery5 + "x" + masteryEmote5.getUsableEmote() + "\n"
            + MessageBuilderRequestUtil.getMasteryUnit(totalNbrMasteries)
            + " **" + LanguageManager.getText(language, "statsProfileTotalPointsRespectSize") + "**\n"
            + MessageBuilderRequestUtil.getMasteryUnit((long) moyennePoints)
            + " **" + LanguageManager.getText(language, "statsProfileAveragePointsRespectSize") + "**", true);


    message.addField(field);

    boolean isRiotApiError = false;
    MatchList matchList = null;

    try {
      matchList = Zoe.getRiotApi().getMatchListByAccountId(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_accoundId, 
          null, null, null, DateTime.now().minusWeeks(1).plusSeconds(10).getMillis(), DateTime.now().getMillis(), -1, -1);
    } catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        throw e;
      }else if (e.getErrorCode() == RiotApiException.UNAVAILABLE || e.getErrorCode() == RiotApiException.SERVER_ERROR) {
        isRiotApiError = true;
      }
      logger.info("Impossible to get match history : {}", e.getMessage());
    }

    if(matchList != null) {
      List<MatchReference> matchsReference = matchList.getMatches();

      List<Match> threeMostRecentMatch = new ArrayList<>();

      if(matchsReference.size() < 3) {
        for(MatchReference matchReference : matchsReference) {
          try {
            threeMostRecentMatch.add(Zoe.getRiotApi().getMatch(leagueAccount.leagueAccount_server,
                matchReference.getGameId()));
          } catch(RiotApiException e) {
            if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
              throw e;
            }
            logger.info("Riot api got an error : {}", e);
          }
        }
      }else {
        for(int i = 0; i < 3; i++) {
          MatchReference matchReference = matchsReference.get(i);

          try {
            threeMostRecentMatch.add(Zoe.getRiotApi().getMatch(leagueAccount.leagueAccount_server, matchReference.getGameId()));
          } catch(RiotApiException e) {
            if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
              throw e;
            }
            logger.info("Riot api got an error : {}", e);
          }
        }
      }

      StringBuilder recentMatchsString = new StringBuilder();

      if(!threeMostRecentMatch.isEmpty()) {
        String unknownTranslated = LanguageManager.getText(language, "unknown");
        for(Match match : threeMostRecentMatch) {
          LocalDateTime matchTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(match.getGameCreation()), ZoneId.ofOffset("UTC", ZoneOffset.UTC));
          Champion champion = new Champion(-1, unknownTranslated, unknownTranslated, null);
          try {
            champion = Ressources.getChampionDataById(match.getParticipantBySummonerId(leagueAccount.leagueAccount_summonerId).getChampionId());
          }catch(NullPointerException e) {
            logger.debug("Data errored, can't detect champion");
          }
          recentMatchsString.append(champion.getEmoteUsable() + " " + champion.getName() + " - **" + MessageBuilderRequestUtil.getPastMoment(matchTime, language) + "**\n");
        }
      }
      field = new Field(latestGameTranslated, recentMatchsString.toString(), true);
    }else {
      if(isRiotApiError) {
        field = new Field(latestGameTranslated, LanguageManager.getText(language, "statsProfileRiotApiError"), true);
      }else {
        field = new Field(latestGameTranslated, LanguageManager.getText(language, "statsProfileNoGamePlayed"), true);
      }
    }

    message.addBlankField(true);
    message.addField(field);


    Set<LeagueEntry> rankPosition = null;
    try {
      rankPosition = Zoe.getRiotApi().getLeagueEntriesBySummonerId(leagueAccount.leagueAccount_server,
          leagueAccount.leagueAccount_summonerId);
    }catch (RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        throw e;
      }
      logger.info("Error with the api : ", e);
    }

    if(rankPosition != null) {

      Iterator<LeagueEntry> iteratorPosition = rankPosition.iterator();

      String unrankedTranslated = LanguageManager.getText(language, "unranked");
      String soloqRank = String.format(LanguageManager.getText(language, "statsProfileQueueSoloq"), unrankedTranslated);
      String flexRank = String.format(LanguageManager.getText(language, "statsProfileQueueFlex"), unrankedTranslated);

      while(iteratorPosition.hasNext()) {
        LeagueEntry leaguePosition = iteratorPosition.next();
        Tier tier = Tier.valueOf(leaguePosition.getTier());
        Rank rank = Rank.valueOf(leaguePosition.getRank());

        FullTier fullTier = new FullTier(tier, rank, leaguePosition.getLeaguePoints());

        if(leaguePosition.getQueueType().equals("RANKED_SOLO_5x5")) {
          soloqRank = String.format(LanguageManager.getText(language, "statsProfileQueueSoloq"), 
              Ressources.getTierEmote().get(tier).getUsableEmote() + " " + fullTier.toString(language));
        } else if(leaguePosition.getQueueType().equals("RANKED_FLEX_SR")) {
          flexRank = String.format(LanguageManager.getText(language, "statsProfileQueueFlex"),
              Ressources.getTierEmote().get(tier).getUsableEmote() + " " + fullTier.toString(language));
        }
      }

      stringBuilder = new StringBuilder();

      stringBuilder.append(soloqRank + "\n");
      stringBuilder.append(flexRank);

      field = new Field(LanguageManager.getText(language, "statsProfileRankedStats"), stringBuilder.toString(), true);
    }else {
      field = new Field(LanguageManager.getText(language, "statsProfileRankedStats"), LanguageManager.getText(language, "unranked"), true);
    }

    message.addField(field);
    message.addBlankField(true);

    if(player != null) {
      message.setImage("attachment://" + player.player_discordId + ".png");
    }else {
      message.setImage("attachment://" + url + ".png");
    }

    message.setColor(new Color(206, 20, 221));

    if(player != null) {
      message.setFooter(String.format(LanguageManager.getText(language, "statsProfileFooterProfileOfPlayer"),
          player.user.getName()), 
          player.user.getAvatarUrl());
    }else {
      message.setFooter(String.format(LanguageManager.getText(language, "statsProfileFooterProfileOfPlayer"),
          leagueAccount.leagueAccount_name, leagueAccount.leagueAccount_name));
    }
    message.setTimestamp(Instant.now());

    return message.build();
  }
}
