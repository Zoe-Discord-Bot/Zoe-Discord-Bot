package ch.kalunight.zoe.migration;

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

public class TFTMigrationScript {
  
  private static final Logger logger = LoggerFactory.getLogger(TFTMigrationScript.class);
  
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
      List<Server> servers = ServerRepository.getAllServers();
      
      numberOfServers = servers.size();
      
      logger.info("Start TFT Migration for {} servers !", numberOfServers);
      
      for(Server server : servers) {
        TreatServer treatServer = new TreatServer(server);
        
        ServerData.getServerExecutor().execute(treatServer);
      }
    } catch (SQLException e) {
      logger.error("CRITITCAL ERROR!", e);
    }
  }
  
  private static class TreatServer implements Runnable {

    private Server server;
    
    public TreatServer(Server server) {
      this.server = server;
    }
    
    @Override
    public void run() {
      
      try {
        List<LeagueAccount> leaguesAccounts = LeagueAccountRepository.getAllLeaguesAccounts(server.serv_guildId);
        
        for(LeagueAccount leagueAccountToTreat : leaguesAccounts) {
          Summoner summoner = Zoe.getRiotApi().getSummonerWithRateLimit(leagueAccountToTreat.leagueAccount_server, leagueAccountToTreat.leagueAccount_summonerId);
          if(summoner != null) {
             
            TFTSummoner tftSummoner = Zoe.getRiotApi().getTFTSummonerByNameWithRateLimit(leagueAccountToTreat.leagueAccount_server, summoner.getName());
            
            if(tftSummoner != null) {
              LeagueAccountRepository.updateAccountTFTDataWithId(leagueAccountToTreat.leagueAccount_id, tftSummoner);
            }else {
              logger.info("delete account {} {} (Can't access anymore to the account)", leagueAccountToTreat.leagueAccount_server.getName(), leagueAccountToTreat.leagueAccount_name);
              LeagueAccountRepository.deleteAccountWithId(leagueAccountToTreat.leagueAccount_id);
            }
          }else {
            logger.info("delete account {} {} (Can't access anymore to the account)", leagueAccountToTreat.leagueAccount_server.getName(), leagueAccountToTreat.leagueAccount_name);
            LeagueAccountRepository.deleteAccountWithId(leagueAccountToTreat.leagueAccount_id);
          }
        }
        
        logger.info("Treatment of the guild {}/{} done !", COUNT_TREATMENT.incrementAndGet(), numberOfServers);
        
      } catch (SQLException e) {
        logger.error("CRITICAL SQL ERROR!", e);
      } catch (Exception e) {
        logger.error("CRITICAL ERROR!", e);
      }
      
    }
    
  }
}
