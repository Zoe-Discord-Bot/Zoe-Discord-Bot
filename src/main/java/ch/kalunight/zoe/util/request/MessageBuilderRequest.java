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
import ch.kalunight.zoe.exception.PlayerNotFoundException;
import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.InfocardPlayerData;
import ch.kalunight.zoe.model.PlayerRankedResult;
import ch.kalunight.zoe.model.RankedChangeType;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedChampionsMastery;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.league.dto.MiniSeries;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.api.endpoints.tft_league.dto.TFTLeagueEntry;
import net.rithms.riot.api.endpoints.tft_match.dto.TFTMatch;
import net.rithms.riot.constant.Platform;

public class MessageBuilderRequest {

  private static final String BLUE_TEAM_STRING = "blueTeam";
  private static final String RED_TEAM_STRING = "redTeam";
  private static final String MASTERIES_WR_THIS_MONTH_STRING = "masteriesWrTitleRespectSize";
  private static final String SOLO_Q_RANK_STRING = "soloqTitleRespectSize";
  private static final String RESUME_OF_THE_GAME_STRING = "resumeOfTheGame";
  private static final String RESUME_OF_LAST_GAME = "resumeOfLastGame";
  private static final String GENERATED_AT_STRING = "generatedAt";

  private static final Logger logger = LoggerFactory.getLogger(MessageBuilderRequest.class);

  private MessageBuilderRequest() {}

  public static PlayerRankedResult getMatchDataMutiplePlayers(LeagueEntry oldEntry, LeagueEntry newEntry, 
      CurrentGameInfo gameOfTheChange, LeagueAccount leagueAccount, String lang, RankedChangeType changeType) {

    SavedMatch match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameOfTheChange.getGameId());

    String accountTitle = null;
    String changeStats = null;
    String statsGame = null;

    FullTier oldFullTier = new FullTier(oldEntry);
    FullTier newFullTier = new FullTier(newEntry);

    String summonerName = LanguageManager.getText(lang, "unknown");

    try {
      summonerName = leagueAccount.getSummoner().getName();
    } catch (RiotApiException e) {
      logger.warn("Error while getting summoner !", e);
    }

    switch(changeType) {
    case BO_CHANGE:
      MiniSeries oldBo = oldEntry.getMiniSeries();
      MiniSeries newBo = newEntry.getMiniSeries();

      if(match.isGivenAccountWinner(leagueAccount.leagueAccount_summonerId)) {
        accountTitle = String.format(LanguageManager.getText(lang, "rankChannelChangeBOProgressWinTitleWithoutGameType"),
            summonerName, oldBo.getProgress().length(),
            oldFullTier.getHeigerDivision().toStringWithoutLp(lang));
      }else {
        accountTitle = String.format(LanguageManager.getText(lang, "rankChannelChangeBOProgressLooseTitleWithoutGameType"),
            summonerName, oldBo.getProgress().length(),
            oldFullTier.getHeigerDivision().toStringWithoutLp(lang));
      }

      changeStats = MessageBuilderRequestUtil.getBoStatus(newBo, lang);
      break;
    case BO_END:
      MiniSeries bo = oldEntry.getMiniSeries();
      boolean boWin;

      if(oldFullTier.getTier() != newFullTier.getTier() || oldFullTier.getRank() != newFullTier.getRank()) {
        boWin = true;
      }else {
        boWin = false;
      }

      if(!boWin) {
        accountTitle = String.format(LanguageManager.getText(lang, "rankChannelChangeBOEndedLooseTitleWithoutGameType"),
            summonerName, bo.getProgress().length(), oldFullTier.getHeigerDivision().toStringWithoutLp(lang));
      }else {
        accountTitle = String.format(LanguageManager.getText(lang, "rankChannelChangeBOEndedWinTitleWithoutGameType"),
            summonerName, bo.getProgress().length(), oldFullTier.getHeigerDivision().toStringWithoutLp(lang));
      }

      changeStats = oldFullTier.toString(lang) + " -> " + newFullTier.toString(lang) + "\n"
          + MessageBuilderRequestUtil.getBoStatus(bo, lang, boWin);
      break;
    case BO_START:
      bo = newEntry.getMiniSeries();
      accountTitle = String.format(LanguageManager.getText(lang, "rankChannelBoStartedTitleWithoutGameType"), 
          summonerName, bo.getProgress().length(),
          newFullTier.getHeigerDivision().toStringWithoutLp(lang));

      changeStats = MessageBuilderRequestUtil.getBoStatus(bo, lang);
      break;
    case ONLY_LP:
      int lpReceived = newEntry.getLeaguePoints() - oldEntry.getLeaguePoints();
      boolean gameWin = (newEntry.getLeaguePoints() - oldEntry.getLeaguePoints()) > 0;

      if(gameWin) {
        accountTitle = String.format(LanguageManager.getText(lang, "rankChannelChangePointOnlyWinTitleWithoutGameType"),
            summonerName, lpReceived);
      }else {
        accountTitle = String.format(LanguageManager.getText(lang, "rankChannelChangePointOnlyLooseTitleWithoutGameType"),
            summonerName, lpReceived * -1);
      }

      changeStats = oldFullTier.toString(lang) + " -> " + newFullTier.toString(lang);
      break;
    case RANK_WITHOUT_BO:
      boolean divisionJump = false;
      boolean goodChange;

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
        if(divisionJump) {
        accountTitle = String.format(LanguageManager.getText(lang, "rankChannelChangeRankChangeWinDivisionSkippedTitleWithoutGameType"),
            summonerName);
        }else {
          accountTitle = String.format(LanguageManager.getText(lang, "rankChannelChangeWonDivisionWithoutGameType"),
              summonerName, newFullTier.toString(lang));
        }
      }else {
        if(divisionJump) {
          accountTitle = String.format(LanguageManager.getText(lang, "rankChannelChangeLooseDivisionDecayTitleWithoutGameType"),
              summonerName);
        }else {
          accountTitle = String.format(LanguageManager.getText(lang, "rankChannelChangeLooseDivisionTitleWithoutGameType"),
              summonerName);
        }
      }

      changeStats = oldFullTier.toString(lang) + " -> " + newFullTier.toString(lang);
      break;
    default:
      return null;
    }

    statsGame = MessageBuilderRequestUtil.getResumeGameStats(leagueAccount, lang, match);

    return new PlayerRankedResult(gameOfTheChange.getGameId(), leagueAccount.leagueAccount_server, accountTitle, changeStats, statsGame);
  }

  public static MessageEmbed createCombinedMessage(List<PlayerRankedResult> playersRankedResult, CurrentGameInfo currentGameInfo, String lang) {

    EmbedBuilder message = new EmbedBuilder();

    String gameType = getGameType(currentGameInfo, lang);

    message.setColor(Color.PINK);
    message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeMultipleResultTitle"), gameType));

    for(PlayerRankedResult playerRankedResult : playersRankedResult) {
      Field field = new Field(playerRankedResult.getCatTitle(), playerRankedResult.getLpResult() + "\n" + playerRankedResult.getGameStats(), false);

      message.addField(field);
    }

    message.setFooter(LanguageManager.getText(lang, GENERATED_AT_STRING));
    message.setTimestamp(Instant.now());

    return message.build();
  }

  public static MessageEmbed createRankChannelCardBoStarted(LeagueEntry newEntry, 
      CurrentGameInfo gameOfTheChange, Player player, LeagueAccount leagueAccount, String lang, JDA jda) throws RiotApiException {

    SavedMatch match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameOfTheChange.getGameId());

    EmbedBuilder message = new EmbedBuilder();

    String gameType = getGameType(gameOfTheChange, lang);

    User user = player.retrieveUser(jda);
    message.setAuthor(user.getName(), null, user.getAvatarUrl());

    MiniSeries bo = newEntry.getMiniSeries();
    FullTier newFullTier = new FullTier(newEntry);

    message.setColor(Color.GREEN);
    message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelBoStartedTitle"), 
        leagueAccount.getSummoner().getName(), bo.getProgress().length(),
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
      CurrentGameInfo gameOfTheChange, Player player, LeagueAccount leagueAccount, String lang, JDA jda) throws RiotApiException {

    SavedMatch match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameOfTheChange.getGameId());

    EmbedBuilder message = new EmbedBuilder();

    MiniSeries oldBo = oldEntry.getMiniSeries();
    MiniSeries newBo = newEntry.getMiniSeries();

    String gameType = getGameType(gameOfTheChange, lang);

    User user = player.retrieveUser(jda);
    message.setAuthor(user.getName(), null, user.getAvatarUrl());

    FullTier oldFullTier = new FullTier(oldEntry);
    try {
      if(match.isGivenAccountWinner(leagueAccount.leagueAccount_summonerId)) {
        message.setColor(Color.GREEN);
        message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeBOProgressWinTitle"),
            leagueAccount.getSummoner().getName(), oldBo.getProgress().length(),
            oldFullTier.getHeigerDivision().toStringWithoutLp(lang), gameType));
      }else {
        message.setColor(Color.RED);
        message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeBOProgressLooseTitle"),
            leagueAccount.getSummoner().getName(), oldBo.getProgress().length(),
            oldFullTier.getHeigerDivision().toStringWithoutLp(lang), gameType));
      }
    }catch(PlayerNotFoundException e) {
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
      CurrentGameInfo gameOfTheChange, Player player, LeagueAccount leagueAccount, String lang, JDA jda) throws RiotApiException {

    SavedMatch match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameOfTheChange.getGameId());

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

    User user = player.retrieveUser(jda);
    message.setAuthor(user.getName(), null, user.getAvatarUrl());

    if(!boWin) {
      message.setColor(Color.RED);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeBOEndedLooseTitle"),
          leagueAccount.getSummoner().getName(), bo.getProgress().length(), oldFullTier.getHeigerDivision().toStringWithoutLp(lang), gameType));
    }else {
      message.setColor(Color.YELLOW);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeBOEndedWinTitle"),
          leagueAccount.getSummoner().getName(), bo.getProgress().length(), oldFullTier.getHeigerDivision().toStringWithoutLp(lang), gameType));
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
      CurrentGameInfo gameOfTheChange, Player player, LeagueAccount leagueAccount, String lang, JDA jda) throws RiotApiException {

    SavedMatch match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameOfTheChange.getGameId());

    EmbedBuilder message = new EmbedBuilder();

    String gameType = getGameType(gameOfTheChange, lang);

    User user = player.retrieveUser(jda);
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
      if(divisionJump) {
      message.setColor(Color.YELLOW);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeRankChangeWinDivisionSkippedTitle"),
          leagueAccount.getSummoner().getName(), gameType));
      }else {
        message.setColor(Color.GREEN);
        message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeWonDivision"),
            leagueAccount.getSummoner().getName(), newFullTier.toString(lang), gameType));
      }
    }else {
      if(divisionJump) {
        message.setColor(Color.BLACK);
        message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeLooseDivisionDecayTitle"),
            leagueAccount.getSummoner().getName(), gameType));
      }else {
        message.setColor(Color.RED);
        message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeLooseDivisionTitle"),
            leagueAccount.getSummoner().getName(), gameType));
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
      CurrentGameInfo gameOfTheChange, Player player, LeagueAccount leagueAccount, String lang, JDA jda) throws RiotApiException {

    SavedMatch match = Zoe.getRiotApi().getMatchWithRateLimit(leagueAccount.leagueAccount_server, gameOfTheChange.getGameId());

    EmbedBuilder message = new EmbedBuilder();

    String gameType = getGameType(gameOfTheChange, lang);

    User user = player.retrieveUser(jda);
    message.setAuthor(user.getName(), null, user.getAvatarUrl());

    int lpReceived = newEntry.getLeaguePoints() - oldEntry.getLeaguePoints();
    boolean gameWin = (newEntry.getLeaguePoints() - oldEntry.getLeaguePoints()) > 0;

    if(gameWin) {
      message.setColor(Color.GREEN);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangePointOnlyWinTitle"),
          leagueAccount.getSummoner().getName(), lpReceived, gameType));
    }else {
      message.setColor(Color.RED);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangePointOnlyLooseTitle"),
          leagueAccount.getSummoner().getName(), lpReceived * -1, gameType));
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

  public static MessageEmbed createRankChannelCardLeaguePointChangeOnlyTFT(LeagueEntry oldEntry, LeagueEntry newEntry, 
      TFTMatch match, Player player, LeagueAccount leagueAccount, String lang, JDA jda) throws NoValueRankException, RiotApiException {

    EmbedBuilder message = new EmbedBuilder();

    String gameType = LanguageManager.getText(lang, GameQueueConfigId.RANKED_TFT.getNameId());

    User user = player.retrieveUser(jda);
    message.setAuthor(user.getName(), null, user.getAvatarUrl());

    FullTier oldFullTier = new FullTier(oldEntry);
    FullTier newFullTier = new FullTier(newEntry);

    int lpReceived = newFullTier.value() - oldFullTier.value();
    boolean gameWin = lpReceived > 0;

    if(gameWin) {
      if(oldFullTier.getTier() != newFullTier.getTier()) {
        message.setColor(Color.YELLOW);
        message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangeRankChangeWinTierTFT"),
            leagueAccount.getSummoner().getName(), lpReceived, LanguageManager.getText(lang, newFullTier.getTier().getTranslationTag()), gameType));
      }else {
        message.setColor(Color.GREEN);
        message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangePointOnlyWinTitle"),
            leagueAccount.getSummoner().getName(), lpReceived, gameType));
      }
    }else {
      message.setColor(Color.RED);
      message.setTitle(String.format(LanguageManager.getText(lang, "rankChannelChangePointOnlyLooseTitle"),
          leagueAccount.getSummoner().getName(), lpReceived * -1, gameType));
    }

    message.setDescription(oldFullTier.toString(lang) + " -> " + newFullTier.toString(lang));

    String statsGame = MessageBuilderRequestUtil.getResumeGameStatsTFT(leagueAccount, lang, match);

    String stringResumeGame = RESUME_OF_LAST_GAME;

    Field field = new Field(LanguageManager.getText(lang, stringResumeGame), statsGame, true);
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
      Platform region, DTO.Server server, JDA jda) throws SQLException {

    String blueTeamTranslated = LanguageManager.getText(server.getLanguage(), BLUE_TEAM_STRING);
    String redTeamTranslated = LanguageManager.getText(server.getLanguage(), RED_TEAM_STRING);
    String masteriesWRThisMonthTranslated = LanguageManager.getText(server.getLanguage(), MASTERIES_WR_THIS_MONTH_STRING);
    String rankTitleTranslated;
    if(currentGameInfo.getGameQueueConfigId() == GameQueueConfigId.FLEX.getId()) {
      rankTitleTranslated = LanguageManager.getText(server.getLanguage(), "flexTitleRespectSize");
    } else {
      rankTitleTranslated = LanguageManager.getText(server.getLanguage(), SOLO_Q_RANK_STRING);
    }

    Set<DTO.LeagueAccount> playersAccountsOfTheGame = new HashSet<>();
    for(DTO.Player player : players) {
      playersAccountsOfTheGame.addAll(
          MessageBuilderRequestUtil.getLeagueAccountsInTheGivenGame(currentGameInfo, region, player, server.serv_guildId));
    }

    EmbedBuilder message = new EmbedBuilder();

    StringBuilder title = new StringBuilder();

    MessageBuilderRequestUtil.createTitle(players, currentGameInfo, title, server.getLanguage(), true, jda);

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

    MessageBuilderRequestUtil.createTeamDataMultipleSummoner(blueTeam, listIdPlayers, region, server.getLanguage(), playersData, true, currentGameInfo.getGameQueueConfigId());
    MessageBuilderRequestUtil.createTeamDataMultipleSummoner(redTeam, listIdPlayers, region, server.getLanguage(), playersData, false, currentGameInfo.getGameQueueConfigId());

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

    message.setFooter(LanguageManager.getText(server.getLanguage(), "infoCardsGameFooter") 
        + " : " + MessageBuilderRequestUtil.getMatchTimeFromDuration(currentGameInfo.getGameLength()), null);

    message.setColor(Color.GREEN);

    return message.build();
  }

  public static MessageEmbed createProfileMessage(DTO.Player player, DTO.LeagueAccount leagueAccount,
      SavedChampionsMastery masteries, String language, String url, JDA jda) throws RiotApiException {    

    String latestGameTranslated = LanguageManager.getText(language, "statsProfileLatestGames");

    EmbedBuilder message = new EmbedBuilder();

    if(player != null) {
      message.setTitle(String.format(LanguageManager.getText(language, "statsProfileTitle"),
          player.retrieveUser(jda).getName(), leagueAccount.getSummoner().getName(),
          leagueAccount.getSummoner().getLevel()));
    }else {
      message.setTitle(String.format(LanguageManager.getText(language, "statsProfileTitle"),
          leagueAccount.getSummoner().getName(), leagueAccount.getSummoner().getName(),
          leagueAccount.getSummoner().getLevel()));
    }

    List<SavedSimpleMastery> threeBestchampionMasteries = StatsProfileCommand.getBestMasteries(masteries, 3);

    StringBuilder stringBuilder = new StringBuilder();

    for(SavedSimpleMastery championMastery : threeBestchampionMasteries) {
      Champion champion = Ressources.getChampionDataById(championMastery.getChampionId());
      stringBuilder.append(champion.getDisplayName() + " " + champion.getName() + " - **" 
          + MessageBuilderRequestUtil.getMasteryUnit(championMastery.getChampionPoints()) +"**\n");
    }

    if(threeBestchampionMasteries.isEmpty()) {
      stringBuilder.append("*" + LanguageManager.getText(language, "empty") + "*");
    }

    Field field = new Field(LanguageManager.getText(language, "statsProfileTopChamp"), stringBuilder.toString(), true);
    message.addField(field);

    int nbrMastery7 = 0;
    int nbrMastery6 = 0;
    int nbrMastery5 = 0;
    long totalNbrMasteries = 0;

    for(SavedSimpleMastery championMastery : masteries.getChampionMasteries()) {
      switch(championMastery.getChampionLevel()) {
      case 5: nbrMastery5++; break;
      case 6: nbrMastery6++; break;
      case 7: nbrMastery7++; break;
      default: break;
      }
      totalNbrMasteries += championMastery.getChampionPoints();
    }

    double moyennePoints = (double) totalNbrMasteries / masteries.getChampionMasteries().size();

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

      List<SavedMatch> threeMostRecentMatch = new ArrayList<>();

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
        for(SavedMatch match : threeMostRecentMatch) {
          LocalDateTime matchTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(match.getGameCreation()), ZoneId.ofOffset("UTC", ZoneOffset.UTC));
          Champion champion = new Champion(-1, unknownTranslated, unknownTranslated, null);
          try {
            champion = Ressources.getChampionDataById(match.getSavedMatchPlayerBySummonerId(leagueAccount.leagueAccount_summonerId).getChampionId());
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

    String unrankedTranslated = LanguageManager.getText(language, "unranked");

    StringBuilder rankStringBuilder = new StringBuilder();

    if(rankPosition != null) {

      Iterator<LeagueEntry> iteratorPosition = rankPosition.iterator();

      String soloqRank = String.format(LanguageManager.getText(language, "statsProfileQueueSoloq"), unrankedTranslated);
      String flexRank = String.format(LanguageManager.getText(language, "statsProfileQueueFlex"), unrankedTranslated);

      while(iteratorPosition.hasNext()) {
        LeagueEntry leaguePosition = iteratorPosition.next();
        Tier tier = Tier.valueOf(leaguePosition.getTier());
        Rank rank = Rank.valueOf(leaguePosition.getRank());

        FullTier fullTier = new FullTier(tier, rank, leaguePosition.getLeaguePoints());

        if(leaguePosition.getQueueType().equals(GameQueueConfigId.SOLOQ.getQueueType())) {
          soloqRank = String.format(LanguageManager.getText(language, "statsProfileQueueSoloq"), 
              Ressources.getTierEmote().get(tier).getUsableEmote() + " " + fullTier.toString(language));
        } else if(leaguePosition.getQueueType().equals(GameQueueConfigId.FLEX.getQueueType())) {
          flexRank = String.format(LanguageManager.getText(language, "statsProfileQueueFlex"),
              Ressources.getTierEmote().get(tier).getUsableEmote() + " " + fullTier.toString(language));
        }
      }

      rankStringBuilder.append(soloqRank + "\n");
      rankStringBuilder.append(flexRank);

    }

    Set<TFTLeagueEntry> tftRankPosition = null;
    try {
        tftRankPosition = Zoe.getRiotApi().getTFTLeagueEntries(leagueAccount.leagueAccount_server,
            leagueAccount.leagueAccount_tftSummonerId);
    }catch (RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        throw e;
      }
      logger.info("Error with the api : ", e);
    }

    if(tftRankPosition != null) {

      Iterator<TFTLeagueEntry> iteratorPosition = tftRankPosition.iterator();

      String tftRank = String.format(LanguageManager.getText(language, "statsProfileQueueTFT"), unrankedTranslated);

      while(iteratorPosition.hasNext()) {
        LeagueEntry leaguePosition = iteratorPosition.next();
        Tier tier = Tier.valueOf(leaguePosition.getTier());
        Rank rank = Rank.valueOf(leaguePosition.getRank());

        FullTier fullTier = new FullTier(tier, rank, leaguePosition.getLeaguePoints());

        if(leaguePosition.getQueueType().equals(GameQueueConfigId.RANKED_TFT.getQueueType())) {
          tftRank = String.format(LanguageManager.getText(language, "statsProfileQueueTFT"), 
              Ressources.getTierEmote().get(tier).getUsableEmote() + " " + fullTier.toString(language));
        }
      }

      rankStringBuilder.append("\n" + tftRank);

      field = new Field(LanguageManager.getText(language, "statsProfileRankedStats"), rankStringBuilder.toString(), true);
    }else {
      String tftRank = String.format(LanguageManager.getText(language, "statsProfileQueueTFT"), unrankedTranslated);
      if(rankPosition != null) {
        rankStringBuilder.append("\n" + tftRank);
        field = new Field(LanguageManager.getText(language, "statsProfileRankedStats"), rankStringBuilder.toString(), true);
      }else {
        field = new Field(LanguageManager.getText(language, "statsProfileRankedStats"), LanguageManager.getText(language, "unranked"), true);
      }
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
          player.retrieveUser(jda).getName()), 
          player.retrieveUser(jda).getAvatarUrl());
    }else {
      message.setFooter(String.format(LanguageManager.getText(language, "statsProfileFooterProfileOfPlayer"),
          leagueAccount.getSummoner().getName(), leagueAccount.getSummoner().getName()));
    }
    message.setTimestamp(Instant.now());

    return message.build();
  }
}
