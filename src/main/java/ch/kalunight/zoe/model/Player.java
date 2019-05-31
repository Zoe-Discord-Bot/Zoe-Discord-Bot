package ch.kalunight.zoe.model;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.Zoe;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class Player {

  private static final Logger logger = LoggerFactory.getLogger(Player.class);

  private User discordUser;
  private List<LeagueAccount> lolAccounts;
  private boolean mentionnable;

  public Player(User discordUser, Summoner summoner, Platform region, boolean mentionnable) {
    this.discordUser = discordUser;
    LeagueAccount lolAccount = new LeagueAccount(summoner, region);
    lolAccounts = new ArrayList<>();
    lolAccounts.add(lolAccount);
    this.mentionnable = mentionnable;
  }

  public Player(User discordUser, List<LeagueAccount> lolAccounts, boolean mentionnable) {
    this.discordUser = discordUser;
    this.lolAccounts = lolAccounts;
    this.mentionnable = mentionnable;
  }

  public List<LeagueAccount> getLeagueAccountsInTheGivenGame(CurrentGameInfo currentGameInfo){
    List<LeagueAccount> lolAccountsInGame = new ArrayList<>();
    for(LeagueAccount leagueAccount : lolAccounts) {
      if(currentGameInfo.getGameId() == leagueAccount.getCurrentGameInfo().getGameId()) {
        lolAccountsInGame.add(leagueAccount);
      }
    }
    return lolAccountsInGame;
  }

  public List<LeagueAccount> getLeagueAccountsInGame() {
    List<LeagueAccount> lolAccountsInGame = new ArrayList<>();
    for(LeagueAccount leagueAccount : lolAccounts) {
      if(leagueAccount.getCurrentGameInfo() != null) {
        lolAccountsInGame.add(leagueAccount);
      }
    }
    return lolAccountsInGame;
  }

  public void refreshAllLeagueAccounts() {
    for(LeagueAccount leagueAccount : lolAccounts) {
      try { 
        leagueAccount.setCurrentGameInfo(Zoe.getRiotApi().getActiveGameBySummoner(leagueAccount.getRegion(), leagueAccount.getSummoner().getId()));
      } catch(RiotApiException e) {
        logger.debug("Impossible to get current game : {}", e.getMessage());
        leagueAccount.setCurrentGameInfo(null);
      }
    }
  }

  public User getDiscordUser() {
    return discordUser;
  }

  public void setDiscordUser(User discordUser) {
    this.discordUser = discordUser;
  }

  public boolean isMentionnable() {
    return mentionnable;
  }

  public void setMentionnable(boolean mentionned) {
    this.mentionnable = mentionned;
  }

  public List<LeagueAccount> getLolAccounts() {
    return lolAccounts;
  }
}
