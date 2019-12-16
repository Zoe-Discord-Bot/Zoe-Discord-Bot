package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Preconditions;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.repositories.CurrentGameInfoRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.constant.CallPriority;

public class InfoPanelRefresherUtil {

  private InfoPanelRefresherUtil() {
    //Hide default public constructor
  }

  public static String getCurrentGameInfoStringForOneAccount(DTO.LeagueAccount account, String language) 
      throws SQLException, RiotApiException {
    Preconditions.checkNotNull(account);

    DTO.CurrentGameInfo currentGameInfo = CurrentGameInfoRepository.getCurrentGameWithLeagueAccountID(account.leagueAccount_id);
    account.summoner = Zoe.getRiotApi().getSummoner(
        account.leagueAccount_server, account.leagueAccount_summonerId, CallPriority.NORMAL);

    String gameStatus = LanguageManager.getText(language, 
        NameConversion.convertGameQueueIdToString(currentGameInfo.currentgame_currentgame.getGameQueueConfigId())) 
        + " " + LanguageManager.getText(language, "withTheAccount") + " **" + account.summoner.getName() + "**";

    double minutesOfGames = 0.0;

    if(currentGameInfo.currentgame_currentgame.getGameLength() != 0l) {
      minutesOfGames = currentGameInfo.currentgame_currentgame.getGameLength() + 180.0;
    }

    minutesOfGames = minutesOfGames / 60.0;
    String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
    int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
    int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

    gameStatus += " (" + minutesGameLength + "m " + secondesGameLength + "s)";

    return gameStatus;
  }

  public static String getCurrentGameInfoStringForMultipleAccounts(List<DTO.LeagueAccount> accounts, String language) 
      throws SQLException, RiotApiException {
    Preconditions.checkNotNull(accounts);

    StringBuilder stringBuilder = new StringBuilder();

    for(DTO.LeagueAccount account : accounts) {
      account.summoner = Zoe.getRiotApi().getSummoner(
          account.leagueAccount_server, account.leagueAccount_summonerId, CallPriority.NORMAL);

      DTO.CurrentGameInfo currentGameInfo = CurrentGameInfoRepository.getCurrentGameWithLeagueAccountID(account.leagueAccount_id);

      stringBuilder.append("-" + LanguageManager.getText(language, "account") 
      + " **" + account.summoner.getName() + "** : ");

      stringBuilder.append(LanguageManager.getText(language,
          NameConversion.convertGameQueueIdToString(currentGameInfo.currentgame_currentgame.getGameQueueConfigId())));

      double minutesOfGames = 0.0;

      if(currentGameInfo.currentgame_currentgame.getGameLength() != 0l) {
        minutesOfGames = currentGameInfo.currentgame_currentgame.getGameLength() + 180.0;
      }

      minutesOfGames = minutesOfGames / 60.0;
      String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
      int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
      int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

      stringBuilder.append(" (" + minutesGameLength + "m " + secondesGameLength + "s)\n");
    }
    return stringBuilder.toString();
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
          server.serv_guildId, leagueAccount.leagueAccount_summonerId, leagueAccount.leagueAccount_server.getName());
      if(!listOfPlayers.contains(player)) {
        listOfPlayers.add(player);
      }
    }
    
    return listOfPlayers;
  }
}