package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.google.common.base.Preconditions;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorParticipant;

public class InfoPanelRefresherUtil {
  
  private InfoPanelRefresherUtil() {
    //Hide default public constructor
  }

  public static String getCurrentGameInfoStringForOneAccount(DTO.LeagueAccount account,
      SpectatorGameInfo currentGameInfo, String language)  {
    Preconditions.checkNotNull(account);
    
    String gameStatus = LanguageManager.getText(language, 
        NameConversion.convertGameQueueIdToString(currentGameInfo.getGameQueueConfig())) 
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
      SpectatorGameInfo> accountsWithCurrentGame, String language) {
    Preconditions.checkNotNull(accountsWithCurrentGame);

    StringBuilder stringBuilder = new StringBuilder();

    for(Entry<LeagueAccount, SpectatorGameInfo> currentGamePerLeagueAccount : accountsWithCurrentGame.entrySet()) {
      
      stringBuilder.append("-" + LanguageManager.getText(language, "account") 
      + " **" + currentGamePerLeagueAccount.getKey().getSummoner().getName() + "** : ");

      stringBuilder.append(LanguageManager.getText(language,
          NameConversion.convertGameQueueIdToString(currentGamePerLeagueAccount.getValue().getGameQueueConfig())));

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

      try {
        if(player.retrieveMember(guild) == null) {
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
    }
  }

  public static List<DTO.LeagueAccount> checkIfOthersAccountsInKnowInTheMatch(
      DTO.CurrentGameInfo currentGameInfo, DTO.Server server) throws SQLException{

    List<DTO.LeagueAccount> allLeaguesAccounts = LeagueAccountRepository.getAllLeaguesAccounts(server.serv_guildId);
    ArrayList<DTO.LeagueAccount> listOfAccounts = new ArrayList<>();

    for(DTO.LeagueAccount leagueAccount : allLeaguesAccounts) {
      for(SpectatorParticipant participant : currentGameInfo.currentgame_currentgame.getParticipants()) {
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