package ch.kalunight.zoe;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import javax.security.auth.login.LoginException;
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
import ch.kalunight.zoe.command.BanAccountCommand;
import ch.kalunight.zoe.command.ConfigCommand;
import ch.kalunight.zoe.command.LanguageCommand;
import ch.kalunight.zoe.command.PatchNotesCommand;
import ch.kalunight.zoe.command.RebootCommand;
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
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.riotapi.CachedRiotApi;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.ZoeMemberCachePolicy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;

public class Zoe {

  public static final String BOT_PREFIX = ">";

  public static final File SAVE_TXT_FILE = new File("ressources/save.txt");

  public static final File RAPI_SAVE_TXT_FILE = new File("ressources/apiInfos.txt");

  public static final File SAVE_CONFIG_FOLDER = new File("ressources/serversconfigs");

  private static final ConcurrentLinkedQueue<List<CustomEmote>> emotesNeedToBeUploaded = new ConcurrentLinkedQueue<>();

  private static final List<Object> eventListenerList = Collections.synchronizedList(new ArrayList<>());
  
  private static CommandClient commandClient = null;

  private static EventWaiter eventWaiter;
  
  public static final Logger logger = LoggerFactory.getLogger(Zoe.class);

  private static final List<GatewayIntent> listOfGatway = Collections.synchronizedList(new ArrayList<>());

  private static List<Command> mainCommands;

  private static CachedRiotApi riotApi;

  private static JDA jda;

  private static String discordTocken;

  private static String discordBotListTocken = "";

  private static String clientOwnerID;

  private static DiscordBotListAPI botListApi;

  static {
    listOfGatway.add(GatewayIntent.DIRECT_MESSAGES);
    listOfGatway.add(GatewayIntent.GUILD_BANS);
    listOfGatway.add(GatewayIntent.GUILD_MEMBERS);
    listOfGatway.add(GatewayIntent.GUILD_MESSAGE_REACTIONS);
    listOfGatway.add(GatewayIntent.GUILD_MESSAGES);
    listOfGatway.add(GatewayIntent.GUILD_PRESENCES);
    listOfGatway.add(GatewayIntent.GUILD_EMOJIS);
  }

  public static void main(String[] args) {

    if(discordTocken != null) { //Avoid strange reboot
      logger.warn("Main method hitted in a strangely Zoe stats ! Avoid execution ...");
      return;
    }

    System.setProperty("logback.configurationFile", "logback.xml");

    CommandClientBuilder client = new CommandClientBuilder();

    String riotTocken;
    String tftTocken;

    try {
      discordTocken = args[0];
      riotTocken = args[1];
      tftTocken = args[2];
      clientOwnerID = args[3];
      client.setOwnerId(clientOwnerID);

      RepoRessources.setupDatabase(args[5], args[4]);
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

    initRiotApi(riotTocken, tftTocken);

    try {
      discordBotListTocken = args[5];
    } catch(Exception e) {
      logger.info("Discord api list tocken not implement");
    }

    client.setPrefix(BOT_PREFIX);

    Consumer<CommandEvent> helpCommand = CommandUtil.getHelpCommand();

    client.setHelpConsumer(helpCommand);

    CommandClient commandClient = client.build();

    SetupEventListener setupEventListener = new SetupEventListener();

    eventListenerList.add(commandClient);
    eventListenerList.add(setupEventListener);
    
    try {
      jda = getNewJDAInstance(discordTocken, commandClient, setupEventListener);//
    } catch(IndexOutOfBoundsException e) {
      logger.error("You must provide a token.");
      System.exit(1);
    } catch(Exception e) {
      logger.error(e.getMessage());
      System.exit(1);
    }
  }

  public static JDA getNewJDAInstance(String riotTocken, CommandClient newCommandClient, SetupEventListener setupEventListener) throws LoginException {

    commandClient = newCommandClient;
    
    JDABuilder builder = JDABuilder.create(discordTocken, getListOfGatway())//
        .setStatus(OnlineStatus.DO_NOT_DISTURB)//
        .disableCache(CacheFlag.CLIENT_STATUS, CacheFlag.VOICE_STATE)
        .setMemberCachePolicy(new ZoeMemberCachePolicy())
        .setChunkingFilter(ChunkingFilter.NONE)
        .addEventListeners(commandClient, setupEventListener);
    
    return builder.build();
  }

  public static void initRiotApi(String riotTocken, String tftTocken) {
    ApiConfig config = new ApiConfig().setKey(riotTocken).setTFTKey(tftTocken);

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
    commands.add(new RebootCommand());

    // Basic commands
    commands.add(new AboutCommand());
    commands.add(new SetupCommand());
    commands.add(new LanguageCommand(eventWaiter));
    commands.add(new ConfigCommand(eventWaiter));
    commands.add(new CreateCommand(eventWaiter));
    commands.add(new DeleteCommand(eventWaiter));
    commands.add(new AddCommand());
    commands.add(new RemoveCommand());
    commands.add(new StatsCommand(eventWaiter));
    commands.add(new ShowCommand(eventWaiter));
    commands.add(new RefreshCommand());
    commands.add(new RegisterCommand());
    commands.add(new DefineCommand());
    commands.add(new UndefineCommand());
    commands.add(new ResetCommand(eventWaiter));
    commands.add(new BanAccountCommand(eventWaiter));
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
  
  public static CommandClient getCommandClient() {
    return commandClient;
  }

  public static void setCommandClient(CommandClient commandClient) {
    Zoe.commandClient = commandClient;
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

  public static void setEventWaiter(EventWaiter eventWaiter) {
    Zoe.eventWaiter = eventWaiter;
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

  public static void setMainCommands(List<Command> mainCommands) {
    Zoe.mainCommands = mainCommands;
  }

  public static List<GatewayIntent> getListOfGatway() {
    return listOfGatway;
  }
}
