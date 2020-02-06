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
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.riotapi.CachedRiotApi;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;

public class Zoe {

  public static final String BOT_PREFIX = ">";

  public static final File SAVE_TXT_FILE = new File("ressources/save.txt");

  public static final File RAPI_SAVE_TXT_FILE = new File("ressources/apiInfos.txt");

  public static final File SAVE_CONFIG_FOLDER = new File("ressources/serversconfigs");

  private static final ConcurrentLinkedQueue<List<CustomEmote>> emotesNeedToBeUploaded = new ConcurrentLinkedQueue<>();

  private static final List<Object> eventListenerList = new ArrayList<>();  

  public static final Logger logger = LoggerFactory.getLogger(Zoe.class);

  private static EventWaiter eventWaiter;

  private static List<Command> mainCommands;

  private static CachedRiotApi riotApi;

  private static JDA jda;

  private static String discordTocken;
  
  private static String discordBotListTocken = "";
  
  private static String clientOwnerID;

  private static DiscordBotListAPI botListApi;

  public static void main(String[] args) {
    
    if(discordBotListTocken != null) { //Avoid strange reboot
      logger.warn("Main method hitted in a strangely Zoe stats ! Avoid execution ...");
      return;
    }

    System.setProperty("logback.configurationFile", "logback.xml");

    CommandClientBuilder client = new CommandClientBuilder();

    String riotTocken;

    try {
      discordTocken = args[0];
      riotTocken = args[1];
      clientOwnerID = args[2];
      client.setOwnerId(clientOwnerID);

      RepoRessources.setDB_URL(args[3]);
      RepoRessources.setDB_PASSWORD(args[4]);
    }catch(Exception e) {
      logger.error("Error with parameters : 1. Discord Tocken 2. Riot tocken 3. Owner Id 4. DB url 5. DB password", e);
      throw e;
    }

    initRiotApi(riotTocken);

    try {
      discordBotListTocken = args[5];
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
      jda.setAutoReconnect(false);
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

    config.setMaxAsyncThreads(ServerData.NBR_PROC);
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

  public static List<Object> getEventlistenerlist() {
    return eventListenerList;
  }

  public static EventWaiter getEventWaiter() {
    return eventWaiter;
  }

  public static void setJda(JDA jda) {
    Zoe.jda = jda;
  }

  public static String getDiscordTocken() {
    return discordTocken;
  }

  public static String getClientOwnerID() {
    return clientOwnerID;
  }

  public static void setEventWaiter(EventWaiter eventWaiter) {
    Zoe.eventWaiter = eventWaiter;
  }

  public static void setMainCommands(List<Command> mainCommands) {
    Zoe.mainCommands = mainCommands;
  }
}
