package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;

public class InfoPanelRefresherUtil {

  private static final Logger logger = LoggerFactory.getLogger(InfoPanelRefresherUtil.class);
  
  private InfoPanelRefresherUtil() {
    //Hide default public constructor
  }

  public static String getCurrentGameInfoStringForOneAccount(DTO.LeagueAccount account,
      CurrentGameInfo currentGameInfo, String language) throws RiotApiException {
    Preconditions.checkNotNull(account);
    
    String gameStatus = LanguageManager.getText(language, 
        NameConversion.convertGameQueueIdToString(currentGameInfo.getGameQueueConfigId())) 
        + " " + LanguageManager.getText(language, "withTheAccount") + " **" + account.getSummoner().getName() + "**";

    double minutesOfGames = 0.0;

    if(currentGameInfo.getGameLength() != 0l) {
      minutesOfGames = currentGameInfo.getGameLength() + 180.0;
    }

    minutesOfGames = minutesOfGames / 60.0;
    String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
    int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
    int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

    gameStatus += " (" + minutesGameLength + "m " + secondesGameLength + "s)";

    return gameStatus;
  }

  public static String getCurrentGameInfoStringForMultipleAccounts(Map<DTO.LeagueAccount, 
      CurrentGameInfo> accountsWithCurrentGame, String language) throws RiotApiException {
    Preconditions.checkNotNull(accountsWithCurrentGame);

    StringBuilder stringBuilder = new StringBuilder();

    for(Entry<LeagueAccount, CurrentGameInfo> currentGamePerLeagueAccount : accountsWithCurrentGame.entrySet()) {
      
      stringBuilder.append("-" + LanguageManager.getText(language, "account") 
      + " **" + currentGamePerLeagueAccount.getKey().getSummoner().getName() + "** : ");

      stringBuilder.append(LanguageManager.getText(language,
          NameConversion.convertGameQueueIdToString(currentGamePerLeagueAccount.getValue().getGameQueueConfigId())));

      double minutesOfGames = 0.0;

      if(currentGamePerLeagueAccount.getValue().getGameLength() != 0l) {
        minutesOfGames = currentGamePerLeagueAccount.getValue().getGameLength() + 180.0;
      }

      minutesOfGames = minutesOfGames / 60.0;
      String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
      int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
      int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

      stringBuilder.append(" (" + minutesGameLength + "m " + secondesGameLength + "s)\n");
    }
    return stringBuilder.toString();
  }

  public static void cleanRegisteredPlayerNoLongerInGuild(Guild guild, List<DTO.Player> listPlayers) throws SQLException {
    Iterator<DTO.Player> iter = listPlayers.iterator();

    while (iter.hasNext()) {
      DTO.Player player = iter.next();

      Stopwatch stopWatch = Stopwatch.createStarted();
      try {
        logger.info("Load user discordID {}", player.player_discordId);
        if(player.getUser(guild.getJDA()) == null) {
          iter.remove();
          PlayerRepository.updateTeamOfPlayerDefineNull(player.player_id);
          PlayerRepository.deletePlayer(player, guild.getIdLong());
        }
      }catch (ErrorResponseException e) {
        if(e.getErrorResponse().equals(ErrorResponse.UNKNOWN_MEMBER) || e.getErrorResponse().equals(ErrorResponse.UNKNOWN_USER)) {
          iter.remove();
          PlayerRepository.updateTeamOfPlayerDefineNull(player.player_id);
          PlayerRepository.deletePlayer(player, guild.getIdLong());
        }
      }catch (NullPointerException e) {
        if(guild != null) {
          iter.remove();
          PlayerRepository.updateTeamOfPlayerDefineNull(player.player_id);
          PlayerRepository.deletePlayer(player, guild.getIdLong());
        }
      }
      stopWatch.stop();
      logger.info("Loading done for {} in {} sec", player.player_discordId, stopWatch.elapsed(TimeUnit.SECONDS));
    }
  }

  public static List<DTO.LeagueAccount> checkIfOthersAccountsInKnowInTheMatch(
      DTO.CurrentGameInfo currentGameInfo, DTO.Server server) throws SQLException{

    List<DTO.LeagueAccount> allLeaguesAccounts = LeagueAccountRepository.getAllLeaguesAccounts(server.serv_guildId);
    ArrayList<DTO.LeagueAccount> listOfAccounts = new ArrayList<>();

    for(DTO.LeagueAccount leagueAccount : allLeaguesAccounts) {
      for(CurrentGameParticipant participant : currentGameInfo.currentgame_currentgame.getParticipants()) {
        if(participant.getSummonerId().equals(leagueAccount.leagueAccount_summonerId) && !listOfAccounts.contains(leagueAccount)) {
          listOfAccounts.add(leagueAccount);
        }
      }
    }
    return listOfAccounts;
  }

  public static List<DTO.Player> checkIfOthersPlayersIsKnowInTheMatch(DTO.CurrentGameInfo currentGameInfo, DTO.Server server)
      throws SQLException {

    ArrayList<DTO.Player> listOfPlayers = new ArrayList<>();
    List<DTO.LeagueAccount> leagueAccounts = checkIfOthersAccountsInKnowInTheMatch(currentGameInfo, server);

    for(DTO.LeagueAccount leagueAccount : leagueAccounts) {
      DTO.Player player = PlayerRepository.getPlayerByLeagueAccountAndGuild(
          server.serv_guildId, leagueAccount.leagueAccount_summonerId, leagueAccount.leagueAccount_server);
      if(!listOfPlayers.contains(player)) {
        listOfPlayers.add(player);
      }
    }

    return listOfPlayers;
  }
}