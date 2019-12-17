package ch.kalunight.zoe;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.discordbots.api.client.DiscordBotListAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.command.AboutCommand;
import ch.kalunight.zoe.command.ConfigCommand;
import ch.kalunight.zoe.command.LanguageCommand;
import ch.kalunight.zoe.command.PatchNotesCommand;
import ch.kalunight.zoe.command.RefreshCommand;
import ch.kalunight.zoe.command.ResetCommand;
import ch.kalunight.zoe.command.SetupCommand;
import ch.kalunight.zoe.command.ShutDownCommand;
import ch.kalunight.zoe.command.add.AddCommand;
import ch.kalunight.zoe.command.admin.AdminCommand;
import ch.kalunight.zoe.command.create.CreateCommand;
import ch.kalunight.zoe.command.create.RegisterCommand;
import ch.kalunight.zoe.command.define.DefineCommand;
import ch.kalunight.zoe.command.define.UndefineCommand;
import ch.kalunight.zoe.command.delete.DeleteCommand;
import ch.kalunight.zoe.command.remove.RemoveCommand;
import ch.kalunight.zoe.command.show.ShowCommand;
import ch.kalunight.zoe.command.stats.StatsCommand;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.model.static_data.CustomEmote;
import ch.kalunight.zoe.riotapi.CachedRiotApi;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.request.ratelimit.PriorityManagerRateLimitHandler;
import net.rithms.riot.api.request.ratelimit.PriorityRateLimit;
import net.rithms.riot.api.request.ratelimit.RateLimitRequestTank;

public class Zoe {

  public static final String BOT_PREFIX = ">";

  public static final File SAVE_TXT_FILE = new File("ressources/save.txt");

  public static final File RAPI_SAVE_TXT_FILE = new File("ressources/apiInfos.txt");

  public static final File SAVE_CONFIG_FOLDER = new File("ressources/serversconfigs");

  private static final ConcurrentLinkedQueue<List<CustomEmote>> emotesNeedToBeUploaded = new ConcurrentLinkedQueue<>();

  private static final List<Object> eventListenerList = new ArrayList<>();  

  public static final Logger logger = LoggerFactory.getLogger(Zoe.class);

  /**
   * USED ONLY FOR STATS ANALYSE. DON'T MODIFY DATA INSIDE.
   */
  private static RateLimitRequestTank minuteApiTank;

  private static EventWaiter eventWaiter;

  private static List<Command> mainCommands;

  private static CachedRiotApi riotApi;

  private static JDA jda;

  private static String discordBotListTocken = "";

  private static DiscordBotListAPI botListApi;

  public static void main(String[] args) {
	  
    System.setProperty("logback.configurationFile", "logback.xml");

    CommandClientBuilder client = new CommandClientBuilder();

    String discordTocken = args[0];
    String riotTocken = args[1];
    client.setOwnerId(args[2]);

    try {
      discordBotListTocken = args[3];
    } catch(Exception e) {
      logger.info("Discord api list tocken not implement");
    }

    client.setPrefix(BOT_PREFIX);

    eventWaiter = new EventWaiter(ServerData.getResponseWaiter(), false);

    for(Command command : getMainCommands(eventWaiter)) {
      client.addCommand(command);
    }

    Consumer<CommandEvent> helpCommand = CommandUtil.getHelpCommand();

    client.setHelpConsumer(helpCommand);

    initRiotApi(riotTocken);

    CommandClient commandClient = client.build();

    EventListener eventListener = new EventListener();

    eventListenerList.add(commandClient);
    eventListenerList.add(eventWaiter);
    eventListenerList.add(eventListener);

    try {
      jda = new JDABuilder(AccountType.BOT)//
          .setToken(discordTocken)//
          .setStatus(OnlineStatus.DO_NOT_DISTURB)//
          .addEventListeners(commandClient)//
          .addEventListeners(eventWaiter)//
          .addEventListeners(eventListener).build();//
    } catch(IndexOutOfBoundsException e) {
      logger.error("You must provide a token.");
      System.exit(1);
    } catch(Exception e) {
      logger.error(e.getMessage());
      System.exit(1);
    }
  }

  private static void initRiotApi(String riotTocken) {
    ApiConfig config = new ApiConfig().setKey(riotTocken);

    PriorityRateLimit secondsLimit = new PriorityRateLimit(50, 25);
    RateLimitRequestTank requestSecondsTank = new RateLimitRequestTank(10, 250, secondsLimit);

    PriorityRateLimit minuteLimit = new PriorityRateLimit(500, 100);
    RateLimitRequestTank requestMinutesTank = new RateLimitRequestTank(600, 15000, minuteLimit);

    List<RateLimitRequestTank> priorityList = new ArrayList<>();
    priorityList.add(requestSecondsTank);
    priorityList.add(requestMinutesTank);

    minuteApiTank = requestMinutesTank;

    //create default priority with dev api key rate limit if no param
    PriorityManagerRateLimitHandler defaultLimite = new PriorityManagerRateLimitHandler(priorityList);

    config.setRateLimitHandler(defaultLimite);
    riotApi = new CachedRiotApi(new RiotApi(config));
  }

  public static List<Command> getMainCommands(EventWaiter eventWaiter) {
    if(mainCommands != null) {
      return mainCommands;
    }
    List<Command> commands = new ArrayList<>();

    // Admin commands
    commands.add(new ShutDownCommand());
    commands.add(new AdminCommand());

    // Basic commands
    commands.add(new AboutCommand());
    commands.add(new SetupCommand());
    commands.add(new LanguageCommand(eventWaiter));
    commands.add(new ConfigCommand(eventWaiter));
    commands.add(new CreateCommand());
    commands.add(new DeleteCommand());
    commands.add(new AddCommand());
    commands.add(new RemoveCommand());
    commands.add(new StatsCommand(eventWaiter));
    commands.add(new ShowCommand(eventWaiter));
    commands.add(new RefreshCommand());
    commands.add(new RegisterCommand());
    commands.add(new DefineCommand());
    commands.add(new UndefineCommand());
    commands.add(new ResetCommand(eventWaiter));
    commands.add(new PatchNotesCommand());

    mainCommands = commands;

    return commands;
  }

  public static void loadChampions() throws IOException {
    List<Champion> champions = new ArrayList<>();

    try(FileReader fr = new FileReader("ressources/champion.json")) {

      JsonObject object = JsonParser.parseReader(fr).getAsJsonObject().get("data").getAsJsonObject();
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

  public static CachedRiotApi getRiotApi() {
    return riotApi;
  }

  public static JDA getJda() {
    return jda;
  }

  public static ConcurrentLinkedQueue<List<CustomEmote>> getEmotesNeedToBeUploaded() {
    return emotesNeedToBeUploaded;
  }

  public static DiscordBotListAPI getBotListApi() {
    return botListApi;
  }

  public static void setBotListApi(DiscordBotListAPI botListApi) {
    Zoe.botListApi = botListApi;
  }

  public static String getDiscordBotListTocken() {
    return discordBotListTocken;
  }

  public static RateLimitRequestTank getMinuteApiTank() {
    return minuteApiTank;
  }

  public static List<Object> getEventlistenerlist() {
    return eventListenerList;
  }

  public static EventWaiter getEventWaiter() {
    return eventWaiter;
  }
}
