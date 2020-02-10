package ch.kalunight.zoe.service;

import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class RankedChannelRefresher implements Runnable {

  private LeagueEntry oldEntry;
  
  private LeagueEntry newEntry;
  
  private CurrentGameInfo gameOfTheChange;
  
  private Player player;
  
  private LeagueAccount leagueAccount;
  
  public RankedChannelRefresher(LeagueEntry oldEntry, LeagueEntry newEntry, CurrentGameInfo gameOfTheChange,
      Player player, LeagueAccount leagueAccount) {
    this.oldEntry = oldEntry;
    this.newEntry = newEntry;
    this.gameOfTheChange = gameOfTheChange;
    this.player = player;
    this.leagueAccount = leagueAccount;
  }
  
  @Override
  public void run() {
    // TODO Auto-generated method stub

  }

}
