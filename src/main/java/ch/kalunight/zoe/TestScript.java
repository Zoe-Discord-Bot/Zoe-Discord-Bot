package ch.kalunight.zoe;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.repositories.ServerRepository;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.endpoints.tft_summoner.dto.TFTSummoner;
import net.rithms.riot.constant.Platform;

public class TestScript {
  
  private static final Logger logger = LoggerFactory.getLogger(TestScript.class);
  
  private static final AtomicInteger COUNT_TREATMENT = new AtomicInteger(0);
  
  private static int numberOfServers = 0;
  
  public static void main(String[] args) {
    
    System.setProperty("logback.configurationFile", "logback.xml");

    String riotTocken;
    String tftTocken;

    try {
      riotTocken = args[0];
      tftTocken = args[1];
      RepoRessources.setDB_URL(args[2]);
      RepoRessources.setDB_PASSWORD(args[3]);
    }catch(Exception e) {
      logger.error("Error with parameters : 1. Discord Tocken 2. LoL tocken 3. TFT tocken 4. Owner Id 5. DB url 6. DB password", e);
      throw e;
    }
    
    try {
      PlayerRepository.setupListOfRegisteredPlayers();
    }catch(SQLException e) {
      logger.error("Error while setup list of registered players", e);
      return;
    }

    Zoe.initRiotApi(riotTocken, tftTocken);
    
    try {
      
      for(int i = 0; i < 15000; i++) {
        TreatServer worker = new TreatServer();
        ServerData.getServerExecutor().execute(worker);
      }
      
    } catch (Exception e) {
      logger.error("CRITITCAL ERROR!", e);
    }
  }
  
  private static class TreatServer implements Runnable {

    private Server server;
    
    public TreatServer() {
      this.server = server;
    }
    
    @Override
    public void run() {
      
      try {
        Zoe.getRiotApi().getTFTSummonerByNameWithRateLimit(Platform.RU, "hashirama12");
        logger.info("Call done {}/{}", COUNT_TREATMENT.incrementAndGet(), 15000);
      } catch (Exception e) {
        logger.error("CRITICAL ERROR!", e);
      }
      
    }
    
  }
}
