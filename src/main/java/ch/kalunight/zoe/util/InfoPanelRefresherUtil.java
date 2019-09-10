package ch.kalunight.zoe.util;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Preconditions;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;

public class InfoPanelRefresherUtil {

  private InfoPanelRefresherUtil() {
    //Hide default public constructor
  }

  public static String getCurrentGameInfoStringForOneAccount(LeagueAccount account) {
    Preconditions.checkNotNull(account);

    String gameStatus = NameConversion.convertGameQueueIdToString(account.getCurrentGameInfo().getGameQueueConfigId()) 
        + " with the account **" + account.getSummoner().getName() + "**";

    double minutesOfGames = 0.0;

    if(account.getCurrentGameInfo().getGameLength() != 0l) {
      minutesOfGames = account.getCurrentGameInfo().getGameLength() + 180.0;
    }

    minutesOfGames = minutesOfGames / 60.0;
    String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
    int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
    int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

    gameStatus += " (" + minutesGameLength + "m " + secondesGameLength + "s)";

    return gameStatus;
  }

  public static String getCurrentGameInfoStringForMultipleAccounts(List<LeagueAccount> accounts) {
    Preconditions.checkNotNull(accounts);

    StringBuilder stringBuilder = new StringBuilder();

    for(LeagueAccount account : accounts) {
      stringBuilder.append("-Account **" + account.getSummoner().getName() + "** : ");

      stringBuilder.append(NameConversion.convertGameQueueIdToString(account.getCurrentGameInfo().getGameQueueConfigId()));

      double minutesOfGames = 0.0;

      if(account.getCurrentGameInfo().getGameLength() != 0l) {
        minutesOfGames = account.getCurrentGameInfo().getGameLength() + 180.0;
      }

      minutesOfGames = minutesOfGames / 60.0;
      String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
      int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
      int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

      stringBuilder.append(" (" + minutesGameLength + "m " + secondesGameLength + "s)\n");
    }
    return stringBuilder.toString();
  }
  
  public static List<LeagueAccount> checkIfOthersAccountsInKnowInTheMatch(CurrentGameInfo currentGameInfo, Server server){

    ArrayList<LeagueAccount> listOfAccounts = new ArrayList<>();

    for(Player player : server.getPlayers()) {
      for(LeagueAccount leagueAccount : player.getLolAccounts()) {
        for(CurrentGameParticipant participant : currentGameInfo.getParticipants()) {
          if(participant.getSummonerId().equals(leagueAccount.getSummoner().getId()) && !listOfAccounts.contains(leagueAccount)) {
            listOfAccounts.add(leagueAccount);
          }
        }
      }
    }
    return listOfAccounts;
  }
  
  public static List<Player> checkIfOthersPlayersIsKnowInTheMatch(CurrentGameInfo currentGameInfo, Server server) {

    ArrayList<Player> listOfPlayers = new ArrayList<>();

    for(Player player : server.getPlayers()) {
      for(LeagueAccount leagueAccount : player.getLolAccounts()) {
        for(CurrentGameParticipant participant : currentGameInfo.getParticipants()) {
          if(participant.getSummonerId().equals(leagueAccount.getSummoner().getId())) {
            listOfPlayers.add(player);
          }
        }
      }
    }
    return listOfPlayers;
  }
}