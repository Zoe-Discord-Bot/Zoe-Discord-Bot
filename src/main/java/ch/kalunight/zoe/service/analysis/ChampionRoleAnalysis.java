package ch.kalunight.zoe.service.analysis;

import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.repositories.SavedMatchCacheRepository;

public class ChampionRoleAnalysis implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ChampionRoleAnalysis.class);
  
  private int championId;
  
  public ChampionRoleAnalysis(int championId) {
    this.championId = championId;
  }
  
  @Override
  public void run() {
    
    try {
      List<SavedMatch> savedMatch = SavedMatchCacheRepository.getMatchsByChampion(championId);
      
      
      
      
    } catch(SQLException e) {
      logger.error("SQL Error with a query");
    }
    
  }

}
