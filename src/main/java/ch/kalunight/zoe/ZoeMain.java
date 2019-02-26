package ch.kalunight.zoe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import ch.kalunight.zoe.model.Champion;
import ch.kalunight.zoe.model.CustomEmote;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.SleeperRateLimitHandler;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.request.ratelimit.RateLimitHandler;
import net.rithms.riot.constant.Platform;

public class ZoeMain {
  
  private static final File SAVE_TXT_FILE = new File("ressources/save.txt");

  private static RiotApi riotApi;
  
  private static JDA jda;
  
  private static final ConcurrentLinkedQueue<List<CustomEmote>> emotesNeedToBeUploaded = new ConcurrentLinkedQueue<>();
  
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
  
  public static void loadChampions() throws IOException {
    JsonParser parser = new JsonParser();
    List<Champion> champions = new ArrayList<>();

    try(FileReader fr = new FileReader("ressources/champion.json")) {

      JsonObject object = parser.parse(fr).getAsJsonObject().get("data").getAsJsonObject();
      Set<Map.Entry<String, JsonElement>> list = object.entrySet();
      Iterator<Map.Entry<String, JsonElement>> iterator = list.iterator();

      while(iterator.hasNext()) {
        JsonElement element = iterator.next().getValue();
        int key = element.getAsJsonObject().get("key").getAsInt();
        String id = element.getAsJsonObject().get("id").getAsString();
        String name = element.getAsJsonObject().get("name").getAsString();
        File championLogo =
            new File("ressources/images/" + element.getAsJsonObject().get("image").getAsJsonObject().get("full").getAsString());
        champions.add(new Champion(key, id, name, championLogo));
      }

    Ressources.setChampions(champions);
    }
  }
  
  public static void loadDataTxt() throws IOException, RiotApiException {
    
    try(BufferedReader reader = new BufferedReader(new FileReader(SAVE_TXT_FILE));) {
      String line;
      
      while((line = reader.readLine()) != null) {
        
        if(line.equalsIgnoreCase("--server")) {
          String guildId = reader.readLine();
          Guild guild = jda.getGuildById(guildId);
          Server server = new Server(guild);
          
          Long nbrPlayers = Long.parseLong(reader.readLine());
          
          List<Player> players = new ArrayList<>();
          
          for(Long i = 0L; i < nbrPlayers; i++) {
            String discordId = reader.readLine();
            String summonerId = reader.readLine();
            String summonerRegion = reader.readLine();
            String mentionableString = reader.readLine();
            
            User user = jda.getUserById(discordId);
            Summoner summoner = riotApi.getSummoner(Platform.getPlatformByName(summonerRegion), summonerId);
            boolean mentionable = Boolean.getBoolean(mentionableString);
            
            players.add(new Player(user, summoner, mentionable));
          }
        }
        
      }
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

  public static ConcurrentLinkedQueue<List<CustomEmote>> getEmotesNeedToBeUploaded() {
    return emotesNeedToBeUploaded;
  }
}
