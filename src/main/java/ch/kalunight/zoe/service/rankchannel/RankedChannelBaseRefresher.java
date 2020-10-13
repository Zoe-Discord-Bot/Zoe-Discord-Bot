package ch.kalunight.zoe.service.rankchannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.RankHistoryChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.player_data.FullTier;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;

public abstract class RankedChannelBaseRefresher implements Runnable {

  protected static final Logger logger = LoggerFactory.getLogger(RankedChannelBaseRefresher.class);

  protected Server server;

  protected RankHistoryChannel rankChannel;

  protected LeagueEntry oldEntry;

  protected LeagueEntry newEntry;

  protected Player player;

  protected LeagueAccount leagueAccount;
  
  public RankedChannelBaseRefresher(RankHistoryChannel rankChannel, LeagueEntry oldEntry, LeagueEntry newEntry,
      Player player, LeagueAccount leagueAccount, Server server) {
    this.rankChannel = rankChannel;
    this.oldEntry = oldEntry;
    this.newEntry = newEntry;
    this.player = player;
    this.leagueAccount = leagueAccount;
    this.server = server;
  }
  
  @Override
  public void run() {
    try {
      
      if(rankChannel == null) {
        return;
      }
      
      if(oldEntry != null) {

        FullTier oldFullTier = new FullTier(oldEntry);
        FullTier newFullTier = new FullTier(newEntry);

        if(oldFullTier.getLeaguePoints() == newFullTier.getLeaguePoints() || newFullTier.getLeaguePoints() == 100 
            || oldFullTier.getLeaguePoints() == 100) { //BO detected OR remake

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

        }else { //No BO
          if(oldFullTier.getRank().equals(newFullTier.getRank()) 
              && oldFullTier.getTier().equals(newFullTier.getTier())) { //Only LP change
            sendLeaguePointChangeOnly();
          }else { //Decay OR division skip OR TFT Massive change
            sendRankChangedWithoutBO();
          }
        }
      }
    } catch (Exception e) {
      logger.error("Unexpected exception in RankedChannelRefresher !", e);
    }
  }  
  
  protected abstract void sendRankChangedWithoutBO();

  protected abstract void sendBOEnded();

  protected abstract void sendBOStarted();

  protected abstract void sendBOInProgess();

  protected abstract void sendLeaguePointChangeOnly();
  
}
