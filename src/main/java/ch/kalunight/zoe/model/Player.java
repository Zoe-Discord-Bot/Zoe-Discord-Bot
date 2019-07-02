package ch.kalunight.zoe.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.Zoe;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class Player {

  private static final Logger logger = LoggerFactory.getLogger(Player.class);

  private User discordUser;
  private List<LeagueAccount> lolAccounts;
  private boolean mentionnable;

  public Player(User discordUser, Summoner summoner, Platform region, boolean mentionnable) {
    this.discordUser = discordUser;
    LeagueAccount lolAccount = new LeagueAccount(summoner, region);
    lolAccounts = Collections.synchronizedList(new ArrayList<>());
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
      if(leagueAccount.getCurrentGameInfo() != null && currentGameInfo.getGameId() == leagueAccount.getCurrentGameInfo().getGameId()) {
        lolAccountsInGame.add(leagueAccount);
      }
    }
    return lolAccountsInGame;
  }
  
  public LeagueAccount getLeagueAccountsBySummonerName(Platform platform, String summonerName) {
    for(LeagueAccount account : lolAccounts) {
      if(account.getRegion().equals(platform) && account.getSummoner().getName().equals(summonerName)) {
        return account;
      }
    }
    return null;
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

  public void refreshAllLeagueAccounts(CallPriority priority) {
    for(LeagueAccount leagueAccount : lolAccounts) {
      try { 
        leagueAccount.setCurrentGameInfo(Zoe.getRiotApi().getActiveGameBySummoner(leagueAccount.getRegion(), leagueAccount.getSummoner().getId(), priority));
      } catch(RiotApiException e) {
        logger.debug("Impossible to get current game : {}", e.getMessage());
        leagueAccount.setCurrentGameInfo(null);
      }
      
      try {
        leagueAccount.setSummoner(Zoe.getRiotApi().getSummoner(leagueAccount.getRegion(), leagueAccount.getSummoner().getId(), priority));
      } catch(RiotApiException e) {
        if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
          logger.info("The summoner has been transfered to a new region, try to recover ...");
          
          for(Platform platform : Platform.values()) {
            try {
              Summoner summoner = Zoe.getRiotApi().getSummonerByPuuid(platform, leagueAccount.getSummoner().getPuuid(), priority);
              leagueAccount.setSummoner(summoner);
              leagueAccount.setRegion(platform);
              break;
            } catch (RiotApiException e1) {
              logger.debug("Account \"{}\" not exist in {}", leagueAccount.getSummoner().getName(), platform.getName());
            }
          }
          
        }else {
          logger.debug("Impossible to refresh the summoner : {}", e.getMessage());
        }
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

  @Override
  public String toString() {
    return "Player [discordUserName=" + discordUser.getName() + ", lolAccounts=" + lolAccounts + "]";
  }
}
