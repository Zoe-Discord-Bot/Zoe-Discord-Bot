package ch.kalunight.zoe.service;

import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.RankHistoryChannel;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.player_data.Rank;
import ch.kalunight.zoe.model.player_data.Tier;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class RankedChannelRefresher implements Runnable {

  private RankHistoryChannel rankChannel;

  private LeagueEntry oldEntry;

  private LeagueEntry newEntry;

  private CurrentGameInfo gameOfTheChange;

  private Player player;

  private LeagueAccount leagueAccount;

  public RankedChannelRefresher(RankHistoryChannel rankChannel, LeagueEntry oldEntry, LeagueEntry newEntry, CurrentGameInfo gameOfTheChange,
      Player player, LeagueAccount leagueAccount) {
    this.oldEntry = oldEntry;
    this.newEntry = newEntry;
    this.gameOfTheChange = gameOfTheChange;
    this.player = player;
    this.leagueAccount = leagueAccount;
  }

  @Override
  public void run() {

    FullTier oldFullTier = new FullTier(Tier.valueOf(oldEntry.getTier()), Rank.valueOf(oldEntry.getRank()), oldEntry.getLeaguePoints());
    FullTier newFullTier = new FullTier(Tier.valueOf(newEntry.getTier()), Rank.valueOf(newEntry.getRank()), newEntry.getLeaguePoints());


    if(oldFullTier.getLeaguePoints() == newFullTier.getLeaguePoints()) { //BO detected OR remake

      if(oldEntry.getMiniSeries() != null && newEntry.getMiniSeries() == null) { //BO ended
        sendBOEnded();
      }else if(oldEntry.getMiniSeries() == null && newEntry.getMiniSeries() != null) { //BO started
        sendBOStarted();
      }else if(oldEntry.getMiniSeries() != null && newEntry.getMiniSeries() != null) { //BO in progress OR remake
        int nbrMatchOldEntry = oldEntry.getMiniSeries().getLosses() + oldEntry.getMiniSeries().getWins();
        int nbrMatchNewEntry = newEntry.getMiniSeries().getLosses() + newEntry.getMiniSeries().getWins();
        
        if(nbrMatchNewEntry != nbrMatchOldEntry) { //BO in progress
          sendBOInProgess();
        }
      }

    }else {
      if(oldFullTier.getRank().equals(newFullTier.getRank())) {
        sendLeaguePointChangeOnly();
      }else {
        sendRankChangedWithoutBO();
      }
    }

  }

  private void sendRankChangedWithoutBO() {
    // TODO Auto-generated method stub
    
  }

  private void sendBOEnded() {
    // TODO Auto-generated method stub
  }

  private void sendBOStarted() {
    // TODO Auto-generated method stub
    
  }

  private void sendBOInProgess() {
    // TODO Auto-generated method stub
    
  }

  private void sendLeaguePointChangeOnly() {
    // TODO Auto-generated method stub

  }

}
