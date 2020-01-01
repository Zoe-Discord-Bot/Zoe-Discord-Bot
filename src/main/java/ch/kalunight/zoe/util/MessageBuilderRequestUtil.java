package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.InfocardPlayerData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.service.SummonerDataWorker;
import ch.kalunight.zoe.translation.LanguageManager;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.constant.Platform;

public class MessageBuilderRequestUtil {

  private static final DecimalFormat df = new DecimalFormat("#.##");

  private MessageBuilderRequestUtil() {
    // Hide default public constructor
  }

  public static List<DTO.LeagueAccount> getLeagueAccountsInTheGivenGame(CurrentGameInfo currentGameInfo, Platform server,
      DTO.Player player, long guildId) throws SQLException{
    
    List<DTO.LeagueAccount> lolAccountsInGame = new ArrayList<>();
    List<DTO.LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccounts(guildId, player.player_discordId);
    
    for(DTO.LeagueAccount leagueAccount : leaguesAccounts) {
      for(CurrentGameParticipant participant : currentGameInfo.getParticipants()) {
        if(participant.getSummonerId().equals(leagueAccount.leagueAccount_summonerId) 
            && server.equals(leagueAccount.leagueAccount_server)) {
          
          lolAccountsInGame.add(leagueAccount);
        }
      }
    }
    return lolAccountsInGame;
  }

  public static void getTeamPlayer(CurrentGameInfo match, int teamID, List<CurrentGameParticipant> teamParticipant,
      List<CurrentGameParticipant> redTeam) {
    for(int i = 0; i < match.getParticipants().size(); i++) {
      if(match.getParticipants().get(i).getTeamId() == teamID) {
        teamParticipant.add(match.getParticipants().get(i));
      } else {
        redTeam.add(match.getParticipants().get(i));
      }
    }
  }

  public static void createTeamDataMultipleSummoner(List<CurrentGameParticipant> teamParticipant, List<String> listIdPlayers,
      Platform platform, String language, List<InfocardPlayerData> playersDataToWait, boolean isBlueTeam) {

    for(CurrentGameParticipant participant : teamParticipant) {
      InfocardPlayerData playerData = new InfocardPlayerData(isBlueTeam);
      SummonerDataWorker playerWorker = new SummonerDataWorker(participant, listIdPlayers, platform, language, playerData);
      ServerData.getPlayersDataWorker().execute(playerWorker);
      playersDataToWait.add(playerData);
    }
  }

  public static void createTitle(List<DTO.Player> players, CurrentGameInfo currentGameInfo, StringBuilder title,
      String language, boolean gameInfo) {
    ArrayList<DTO.Player> playersNotTwice = new ArrayList<>();

    for(DTO.Player player : players) {
      if(!playersNotTwice.contains(player)) {
        playersNotTwice.add(player);
      }
    }

    title.append(LanguageManager.getText(language, "infoCardsGameInfoOnTheGameOf"));

    String andOfTranslated = LanguageManager.getText(language, "infoCardsGameInfoAndOf");

    for(int i = 0; i < playersNotTwice.size(); i++) {
      if(i == 0) {
        title.append(" " + playersNotTwice.get(i).user.getName());
      } else if(i + 1 == playersNotTwice.size()) {
        title.append(" " + andOfTranslated + " " + playersNotTwice.get(i).user.getName());
      } else if(i + 2 == playersNotTwice.size()) {
        title.append(" " + playersNotTwice.get(i).user.getName());
      } else {
        title.append(" " + playersNotTwice.get(i).user.getName() + ",");
      }
    }

    if(gameInfo) {
      title.append(" : " + LanguageManager.getText(language, NameConversion.convertGameQueueIdToString(currentGameInfo.getGameQueueConfigId())));
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
}
