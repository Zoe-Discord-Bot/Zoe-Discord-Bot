package ch.kalunight.zoe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import ch.kalunight.zoe.util.SleeperRateLimitHandler;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.request.ratelimit.RateLimitHandler;

public class ZoeMain {

  private static RiotApi riotApi;
  
  private static JDA jda;
  
  private static final Logger logger = LoggerFactory.getLogger(ZoeMain.class);
  
  public static void main(String[] args) {

    System.setProperty("logback.configurationFile", "logback.xml");

    String discordTocken = args[0];
    String riotTocken = args[1];

    CommandClientBuilder client = new CommandClientBuilder();

    client.setPrefix(">");

    client.setOwnerId(args[2]);
    
    client.addCommands();
    
    ApiConfig config = new ApiConfig().setKey(riotTocken);

    RateLimitHandler defaultLimite = new SleeperRateLimitHandler();

    config.setRateLimitHandler(defaultLimite);
    riotApi = new RiotApi(config);
    
    try {
      jda = new JDABuilder(AccountType.BOT)//
          .setToken(discordTocken)//
          .setStatus(OnlineStatus.DO_NOT_DISTURB)//
          .addEventListener(client.build())//
          .build();//
    } catch(IndexOutOfBoundsException e) {
      logger.error("You must provide a token.");
      System.exit(1);
    } catch(Exception e) {
      logger.error(e.getMessage());
      System.exit(1);
    }
  }

  public static RiotApi getRiotApi() {
    return riotApi;
  }

  public static void setRiotApi(RiotApi riotApi) {
    ZoeMain.riotApi = riotApi;
  }

  public static JDA getJda() {
    return jda;
  }

  public static void setJda(JDA jda) {
    ZoeMain.jda = jda;
  }
}
