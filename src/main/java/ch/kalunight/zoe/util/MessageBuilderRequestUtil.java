package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.exception.PlayerNotFoundException;
import ch.kalunight.zoe.model.InfocardPlayerData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.service.infochannel.SummonerDataWorker;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.JDA;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.pojo.lol.league.MiniSeries;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorParticipant;
import no.stelar7.api.r4j.pojo.tft.TFTMatch;
import no.stelar7.api.r4j.pojo.tft.TFTParticipant;

public class MessageBuilderRequestUtil {

  private static final DecimalFormat df = new DecimalFormat("#.##");

  private static final DateTimeFormatter minutesSecondFormat = new DateTimeFormatterBuilder().appendPattern("mm:ss").toFormatter();

  private MessageBuilderRequestUtil() {
    // Hide default public constructor
  }

  public static List<DTO.LeagueAccount> getLeagueAccountsInTheGivenGame(SpectatorGameInfo currentGameInfo, ZoePlatform server,
      DTO.Player player, long guildId) throws SQLException{

    List<DTO.LeagueAccount> lolAccountsInGame = new ArrayList<>();
    List<DTO.LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccounts(guildId, player.player_discordId);

    for(DTO.LeagueAccount leagueAccount : leaguesAccounts) {
      for(SpectatorParticipant participant : currentGameInfo.getParticipants()) {
        if(participant.getSummonerId().equals(leagueAccount.leagueAccount_summonerId) 
            && server.equals(leagueAccount.leagueAccount_server)) {

          lolAccountsInGame.add(leagueAccount);
        }
      }
    }
    return lolAccountsInGame;
  }

  public static void getTeamPlayer(SpectatorGameInfo match, int teamID, List<SpectatorParticipant> teamParticipant,
      List<SpectatorParticipant> redTeam) {
    for(int i = 0; i < match.getParticipants().size(); i++) {
      if(match.getParticipants().get(i).getTeam().getValue() == teamID) {
        teamParticipant.add(match.getParticipants().get(i));
      } else {
        redTeam.add(match.getParticipants().get(i));
      }
    }
  }

  public static void createTeamDataMultipleSummoner(List<SpectatorParticipant> teamParticipant, List<String> listIdPlayers,
      ZoePlatform platform, String language, List<InfocardPlayerData> playersDataToWait, boolean isBlueTeam, GameQueueType gameQueueConfigId) {

    for(SpectatorParticipant participant : teamParticipant) {
      InfocardPlayerData playerData = new InfocardPlayerData(isBlueTeam);
      SummonerDataWorker playerWorker = new SummonerDataWorker(participant, listIdPlayers, platform, language, playerData, gameQueueConfigId);
      ServerThreadsManager.getPlayersDataWorker(platform).execute(playerWorker);
      playersDataToWait.add(playerData);
    }
  }

  public static void createTitle(List<DTO.Player> players, SpectatorGameInfo currentGameInfo, StringBuilder title,
      String language, boolean gameInfo, JDA jda) {
    ArrayList<DTO.Player> playersNotTwice = new ArrayList<>();

    for(DTO.Player player : players) {
      if(!playersNotTwice.contains(player)) {
        playersNotTwice.add(player);
      }
    }

    addListOfPlayersInGivenString(title, language, jda, playersNotTwice);
    
    if(gameInfo) {
      title.append(" : " + LanguageManager.getText(language, NameConversion.convertGameQueueIdToString(currentGameInfo.getGameQueueConfig())));
    }
  }

  private static void addListOfPlayersInGivenString(StringBuilder title, String language, JDA jda,
      ArrayList<DTO.Player> playersNotTwice) {
    title.append(LanguageManager.getText(language, "infoCardsGameInfoOnTheGameOf") + " ");

    String andOfTranslated = LanguageManager.getText(language, "infoCardsGameInfoAndOf");
    
    getReadableListOfPlayers(title, jda, playersNotTwice, andOfTranslated);
  }

  public static void getReadableListOfPlayers(StringBuilder baseString, JDA jda, ArrayList<DTO.Player> playersNotTwice,
      String andOfTranslated) {
    switch (playersNotTwice.size()) {
    case 1:
      baseString.append(playersNotTwice.get(0).retrieveUser(jda).getName());
      break;

    case 2:
      baseString.append(playersNotTwice.get(0).retrieveUser(jda).getName() + " "
          + andOfTranslated + " " + playersNotTwice.get(1).retrieveUser(jda).getName());
      break;
    default:
      
      for(int i = 0; i < playersNotTwice.size(); i++) {
        if(i + 1 == playersNotTwice.size()) {
          baseString.append(" " + andOfTranslated + " " + playersNotTwice.get(i).retrieveUser(jda).getName());
        } else if(i + 2 == playersNotTwice.size()) {
          baseString.append(" " + playersNotTwice.get(i).retrieveUser(jda).getName());
        } else {
          if(i == 0) {
            baseString.append(playersNotTwice.get(i).retrieveUser(jda).getName() + ",");
          }else {
            baseString.append(" " + playersNotTwice.get(i).retrieveUser(jda).getName() + ",");
          }
        }
      }
      break;
    }
  }

  public static String getMasteryUnit(Long masteryPoints) {
    if(masteryPoints > 1000 && masteryPoints < 1000000) {
      return masteryPoints / 1000 + "k";
    } else if(masteryPoints > 1000000) {
      return df.format((double) masteryPoints / 1000000) + "m";
    }
    return masteryPoints.toString();
  }

  //TODO: Improve this method, make it automated adaptable
  public static String getPastMoment(LocalDateTime pastMoment, String language) {
    LocalDateTime now = LocalDateTime.now();
    if(pastMoment.isBefore(now.minusWeeks(1))) {
      return LanguageManager.getText(language, "aWeekAgo");
    }else if(pastMoment.isBefore(now.minusDays(6))) {
      return LanguageManager.getText(language, "6DaysAgo");
    }else if(pastMoment.isBefore(now.minusDays(5))) {
      return LanguageManager.getText(language, "5DaysAgo");
    }else if(pastMoment.isBefore(now.minusDays(4))) {
      return LanguageManager.getText(language, "4DaysAgo");
    }else if(pastMoment.isBefore(now.minusDays(3))) {
      return LanguageManager.getText(language, "3DaysAgo");
    }else if(pastMoment.isBefore(now.minusDays(2))) {
      return LanguageManager.getText(language, "2DaysAgo");
    }else if(pastMoment.isBefore(now.minusDays(1))) {
      return LanguageManager.getText(language, "yesterday");
    }else if(pastMoment.isBefore(now.minusHours(6))) {
      return LanguageManager.getText(language, "today");
    }else if(pastMoment.isBefore(now.minusHours(5))) {
      return LanguageManager.getText(language, "5HoursAgo");
    }else if(pastMoment.isBefore(now.minusHours(4))) {
      return LanguageManager.getText(language, "4HoursAgo");
    }else if(pastMoment.isBefore(now.minusHours(3))) {
      return LanguageManager.getText(language, "3HoursAgo");
    }else if(pastMoment.isBefore(now.minusHours(2))) {
      return LanguageManager.getText(language, "2HoursAgo");
    }else if(pastMoment.isBefore(now.minusHours(1))) {
      return LanguageManager.getText(language, "1HourAgo");
    }else if(pastMoment.isBefore(now.minusMinutes(30))) {
      return LanguageManager.getText(language, "30MinutesAgo");
    }else if(pastMoment.isBefore(now.minusMinutes(10))) {
      return LanguageManager.getText(language, "fewsMinutesAgo");
    }else {
      return LanguageManager.getText(language, "unknown");
    }
  }

  public static String getResumeGameStatsTFT(LeagueAccount leagueAccount, String lang, TFTMatch match) {
    StringBuilder statsGame = new StringBuilder();

    TFTParticipant participant = null;
    
    for(TFTParticipant participantToCheck : match.getParticipants()) {
      if(participantToCheck.getPuuid().equals(leagueAccount.leagueAccount_tftPuuid)) {
        participant = participantToCheck;
        break;
      }
    }
    
    if(participant == null) {
      return LanguageManager.getText(lang, "Error");
    }

    statsGame.append(String.format(LanguageManager.getText(lang, "numberPlace"), participant.getPlacement()) + " | ");

    statsGame.append((participant.getLevel() + " " + LanguageManager.getText(lang, "level")) + " | ");

    if(participant.getPlayersEliminated() == 0 || participant.getPlayersEliminated() == 1) {
      statsGame.append(String.format(LanguageManager.getText(lang, "playerKilled"), participant.getPlayersEliminated()) + " | ");
    }else {
      statsGame.append(String.format(LanguageManager.getText(lang, "playersKilled"), participant.getPlayersEliminated()) + " | ");
    }

    statsGame.append(String.format(LanguageManager.getText(lang, "playerDamageGiven"), participant.getTotalDamageToPlayers()) + " | ");

    LocalTime timeOfDay = LocalTime.ofSecondOfDay((long) participant.getTimeEliminated());
    statsGame.append(timeOfDay.format(minutesSecondFormat));

    return statsGame.toString();
  }

  public static String getResumeGameStats(LeagueAccount leagueAccount, String lang, SavedMatch match) {
    StringBuilder statsGame = new StringBuilder();

    SavedMatchPlayer participant = null;
    
    for(SavedMatchPlayer participantToCheck : match.getPlayers()) {
      if(participantToCheck.getSummonerId().equals(leagueAccount.leagueAccount_summonerId)) {
        participant = participantToCheck;
        break;
      }
    }

    Champion champion = Ressources.getChampionDataById(participant.getChampionId());

    if(champion != null) {
      statsGame.append(champion.getDisplayName());
    }else {
      statsGame.append("Unknown");
    }

    String gameDuration = MessageBuilderRequestUtil.getMatchTimeFromDuration(match.getGameDurations());

    String showableResult = getParticipantMatchResult(lang, participant, match);

    int totalcs = participant.getCreepScores();

    statsGame.append(" | " + participant.getKills() + "/" + participant.getDeaths() + "/" + participant.getAssists() 
    + " | " + totalcs + " " + LanguageManager.getText(lang, "creepScoreAbreviation")
    + " | " + LanguageManager.getText(lang, "level") + " " + participant.getLevel()
    + " | " + gameDuration
    + " | " + showableResult);
    return statsGame.toString();
  }

  private static String getParticipantMatchResult(String lang, SavedMatchPlayer participant, SavedMatch match) {
    String showableResult;

    try {
      if(participant.didWin(match)) {
        showableResult = LanguageManager.getText(lang, "win");
      }else {
        showableResult = LanguageManager.getText(lang, "loose");
      }
    }catch (PlayerNotFoundException e) {
      showableResult = LanguageManager.getText(lang, "canceled");
    }

    return showableResult;
  }

  public static String getMatchTimeFromDuration(long duration) {
    double minutesOfGames = duration;

    minutesOfGames = minutesOfGames / 60.0;
    String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
    int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
    int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

    return String.format("%02d", minutesGameLength) + ":" + String.format("%02d", secondesGameLength);
  }
  
  public static String getMatchTimeFromDurationInGame(long duration) {
    double minutesOfGames = 0.0;

    if(duration != 0l) {
      minutesOfGames = duration + 180.0;
    }

    minutesOfGames = minutesOfGames / 60.0;
    String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
    int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
    int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);
    
    return String.format("%02d", minutesGameLength) + ":" + String.format("%02d", secondesGameLength);
  }

  public static String getBoStatus(MiniSeries bo, String lang, boolean lastWin) {

    StringBuilder boStatus = new StringBuilder();

    boStatus.append(LanguageManager.getText(lang, "rankChannelChangeBOProgressDesc") + " ");

    boolean lastWinChanged = false;

    for(int i = 0; i < bo.getProgress().length(); i++) {
      char progressPart = bo.getProgress().charAt(i);

      if(progressPart == 'W') {
        boStatus.append("✅");
      } else if(progressPart == 'L') {
        boStatus.append(Ressources.getGameLost().getUsableEmote());
      } else if(progressPart == 'N') {
        if(!lastWinChanged && lastWin) {
          boStatus.append("✅");
          lastWinChanged = true;
        }else if(!lastWinChanged && !lastWin){
          boStatus.append(Ressources.getGameLost().getUsableEmote());
          lastWinChanged = true;
        }else {
          boStatus.append(Ressources.getGameToDo().getUsableEmote());
        }
      }

      if((i + 1) != bo.getProgress().length()) {
        boStatus.append("->");
      }
    }

    return boStatus.toString();
  }

  public static String getBoStatus(MiniSeries bo, String lang) {

    StringBuilder boStatus = new StringBuilder();

    boStatus.append(LanguageManager.getText(lang, "rankChannelChangeBOProgressDesc") + " ");

    for(int i = 0; i < bo.getProgress().length(); i++) {
      char progressPart = bo.getProgress().charAt(i);

      if(progressPart == 'W') {
        boStatus.append("✅");
      } else if(progressPart == 'L') {
        boStatus.append(Ressources.getGameLost().getUsableEmote());
      } else if(progressPart == 'N') {
        boStatus.append(Ressources.getGameToDo().getUsableEmote());
      }

      if((i + 1) != bo.getProgress().length()) {
        boStatus.append("->");
      }
    }

    return boStatus.toString();
  }
}
