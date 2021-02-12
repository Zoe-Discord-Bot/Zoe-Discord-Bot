package ch.kalunight.zoe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
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
import ch.kalunight.zoe.command.clash.ClashCommand;
import ch.kalunight.zoe.command.create.CreateCommand;
import ch.kalunight.zoe.command.create.RegisterCommand;
import ch.kalunight.zoe.command.define.DefineCommand;
import ch.kalunight.zoe.command.define.UndefineCommand;
import ch.kalunight.zoe.command.delete.DeleteCommand;
import ch.kalunight.zoe.command.remove.RemoveCommand;
import ch.kalunight.zoe.command.show.ShowCommand;
import ch.kalunight.zoe.command.stats.StatsCommand;
import ch.kalunight.zoe.model.dangerosityreport.DangerosityReportKDA;
import ch.kalunight.zoe.model.dto.DTO.ChampionRoleAnalysis;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.model.static_data.CustomEmote;
import ch.kalunight.zoe.repositories.ChampionRoleAnalysisRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.riotapi.CacheManager;
import ch.kalunight.zoe.riotapi.CachedRiotApi;
import ch.kalunight.zoe.service.RiotApiUsageChannelRefresh;
import ch.kalunight.zoe.service.ServerChecker;
import ch.kalunight.zoe.service.analysis.ChampionRole;
import ch.kalunight.zoe.service.analysis.ChampionRoleAnalysisMainWorker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.EventListenerUtil;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.ZoeMemberCachePolicy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
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

  public static final int NUMBER_OF_SHARDS = 4;

  private static boolean zoeIsBooted = false;

  private static final ConcurrentLinkedQueue<List<CustomEmote>> emotesNeedToBeUploaded = new ConcurrentLinkedQueue<>();

  private static final List<Object> eventListenerList = Collections.synchronizedList(new ArrayList<>());

  private static CommandClient commandClient = null;

  private static EventWaiter eventWaiter;

  public static final Logger logger = LoggerFactory.getLogger(Zoe.class);

  private static final List<GatewayIntent> listOfGatway = Collections.synchronizedList(new ArrayList<>());

  private static List<Command> mainCommands;

  private static CachedRiotApi riotApi;

  private static List<JDA> clientsLoaded = new ArrayList<>();

  private static String discordTocken;

  private static String discordBotListTocken = null;

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
      discordBotListTocken = args[6];
    } catch(Exception e) {
      logger.info("Discord api list tocken not implement");
    }

    client.setPrefix(BOT_PREFIX);

    Consumer<CommandEvent> helpCommand = CommandUtil.getHelpCommand();

    client.setHelpConsumer(helpCommand);

    CommandClient commandClient = client.build();

    EventWaiter eventWaiter = new EventWaiter(ServerThreadsManager.getResponseWaiter(), false);

    Zoe.setEventWaiter(eventWaiter);

    SetupEventListener setupEventListener = new SetupEventListener();

    eventListenerList.add(commandClient); //commands set in loadZoeRessources
    eventListenerList.add(setupEventListener);

    try {
      clientsLoaded = getNewJDAInstance(discordTocken, commandClient, setupEventListener);
    } catch(IndexOutOfBoundsException e) {
      logger.error("You must provide a token.");
      System.exit(1);
    } catch(Exception e) {
      logger.error(e.getMessage());
      System.exit(1);
    }

    logger.info("Wait load of all shards...");

    waitLoadingOfAllShards();

    logger.info("Shards all loaded ! Now loading Zoe ressources...");

    loadZoeRessources();

    logger.info("Ressources loaded ! Zoe has booted correctly !");
    zoeIsBooted = true;
  }

  private static void waitLoadingOfAllShards() {
    for(JDA clientToWait : clientsLoaded) {
      try {
        clientToWait.awaitReady();
      } catch (InterruptedException e) {
        logger.error("Error while waiting a shard to load", e);
        System.exit(1);
        Thread.currentThread().interrupt();
      }
    }
  }

  private static void loadZoeRessources() {
    logger.info("Loading of translations ...");
    try {
      LanguageManager.loadTranslations();
    } catch(IOException e) {
      logger.error("Critical error with the loading of translations (File issue) !", e);
      System.exit(1);
    }
    logger.info("Loading of translation finished !");

    logger.info("Loading of champions ...");
    try {
      Zoe.loadChampions();
    } catch(IOException e) {
      logger.error("Critical error with the loading of champions !", e);
      System.exit(1);
    }

    logger.info("Loading of champions finished !");

    logger.info("Loading of emotes ...");
    try {
      EventListenerUtil.loadCustomEmotes();
      logger.info("Loading of emotes finished !");
    } catch(IOException e) {
      logger.warn("Error with the loading of emotes : {}", e.getMessage());
    }

    logger.info("Setup cache ...");
    CacheManager.setupCache();
    logger.info("Setup cache finished !");

    logger.info("Loading of RAPI Status Channel ...");

    initRAPIStatusChannel();

    logger.info("Loading of RAPI Status Channel finished !");

    logger.info("Loading of DiscordBotList API ...");

    try {
      Zoe.setBotListApi(new DiscordBotListAPI.Builder().botId(getSelfUserId()).token(Zoe.getDiscordBotListTocken()) // SET
          .build());                                                                                                // TOCKEN
      logger.info("Loading of DiscordBotList API finished !");
    } catch(Exception e) {
      logger.info("Discord bot list api not loaded normally ! Working of the bot not affected");
      Zoe.setBotListApi(null);
    }

    logger.info("Setup of main thread  ...");
    setupContinousRefreshThread();
    logger.info("Setup of main thread finished !");

    logger.info("Setup of commands ...");
    for(Command command : Zoe.getMainCommands(eventWaiter)) {
      commandClient.addCommand(command);
    }
    logger.info("Setup of commands done !");

    logger.info("Setup of EventListener and add EventWaiter...");
    EventListener eventListener = new EventListener();
    for(JDA clientToUpdate : clientsLoaded) {
      clientToUpdate.addEventListener(eventListener, eventWaiter);
    }
    logger.info("Setup of EventListener and addition EventWaiter done !");

    PlayerRepository.getLoadedGuild().clear();
  }

  public static List<JDA> getNewJDAInstance(String riotTocken, CommandClient newCommandClient, SetupEventListener setupEventListener) throws LoginException {

    commandClient = newCommandClient;

    JDABuilder builder = JDABuilder.create(discordTocken, getListOfGatway())//
        .setStatus(OnlineStatus.DO_NOT_DISTURB)//
        .disableCache(CacheFlag.CLIENT_STATUS, CacheFlag.VOICE_STATE)
        .setMemberCachePolicy(new ZoeMemberCachePolicy())
        .setChunkingFilter(ChunkingFilter.NONE)
        .addEventListeners(commandClient, setupEventListener);

    List<JDA> clientsLoaded = Collections.synchronizedList(new ArrayList<>());

    logger.info("Start the loading of {} discord shards.", NUMBER_OF_SHARDS);
    for(int i = 0; i < NUMBER_OF_SHARDS; i++) {
      clientsLoaded.add(builder.useSharding(i, NUMBER_OF_SHARDS).build());
    }

    return clientsLoaded;
  }

  public static void initRiotApi(String riotTocken, String tftTocken) {
    ApiConfig config = new ApiConfig().setKey(riotTocken).setTFTKey(tftTocken);

    config.setMaxAsyncThreads(ServerThreadsManager.NBR_PROC);
    riotApi = new CachedRiotApi(new RiotApi(config));
  }

  private static void setupContinousRefreshThread() {
    TimerTask mainThread = new ServerChecker();
    ServerThreadsManager.getServerCheckerThreadTimer().schedule(mainThread, 10000);
  }

  public static synchronized List<Command> getMainCommands(EventWaiter eventWaiter) {
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
    commands.add(new ClashCommand());
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

    loadRoleChampions(champions);
  }

  private static void loadRoleChampions(List<Champion> champions) {
    List<ChampionRole> allRoles = new ArrayList<>();
    allRoles.addAll(Arrays.asList(ChampionRole.values()));

    for(Champion champion : champions) {
      try {
        ChampionRoleAnalysis championRole = ChampionRoleAnalysisRepository.getChampionRoleAnalysis(champion.getKey());

        if(championRole != null) {
          champion.setRoles(championRole.cra_roles);
          champion.setAverageKDA(championRole.cra_average_kda);
        }else {
          champion.setRoles(allRoles);
          champion.setAverageKDA(DangerosityReportKDA.DEFAULT_AVERAGE_KDA);
          ChampionRoleAnalysisMainWorker roleAnalyser = new ChampionRoleAnalysisMainWorker(champion.getKey());
          ServerThreadsManager.getDataAnalysisManager().execute(roleAnalyser);
        }
      }catch (SQLException e) {
        champion.setRoles(allRoles);
      }
    }
  }

  private static void initRAPIStatusChannel() {
    try(final BufferedReader reader = new BufferedReader(new FileReader(Zoe.RAPI_SAVE_TXT_FILE));) {
      String line;

      List<String> args = new ArrayList<>();

      while((line = reader.readLine()) != null) {
        args.add(line);
      }

      if(args.size() == 2) {
        Guild guild = Zoe.getGuildById(args.get(0));
        if(guild != null) {
          TextChannel rapiStatusChannel = guild.getTextChannelById(args.get(1));
          if(rapiStatusChannel != null) {
            RiotApiUsageChannelRefresh.setTextChannelId(rapiStatusChannel.getIdLong());
            RiotApiUsageChannelRefresh.setGuildId(guild.getIdLong());
            logger.info("RAPI Status channel correctly loaded.");
          }
        }
      }      
    } catch(FileNotFoundException e1) {
      logger.info("Needed file doesn't exist. Will be created if needed.");
    } catch(IOException e1) {
      logger.warn("Error when loading the file of RAPI Status Channel. The older channel will be unused ! (You can re-create it)");
    }
  }

  public static CachedRiotApi getRiotApi() {
    return riotApi;
  }

  public static JDA getJdaByGuildId(long guildId) {
    for(JDA client : clientsLoaded) {
      if(client.getGuildById(guildId) != null) {
        return client;
      }
    }

    return null;
  }

  public static Guild getGuildById(long guildId) {
    for(JDA client : clientsLoaded) {
      Guild guild = client.getGuildById(guildId);
      if(guild != null) {
        return guild;
      }
    }

    return null;
  }

  public static Guild getGuildById(String guildId) {
    for(JDA client : clientsLoaded) {
      Guild guild = client.getGuildById(guildId);
      if(guild != null) {
        return guild;
      }
    }

    return null;
  }

  public static User getUserById(long userId) {
    for(JDA client : clientsLoaded) {
      User user = client.retrieveUserById(userId).complete();
      if(user != null) {
        return user;
      }
    }

    return null;
  }

  public static String getSelfUserId() {
    for(JDA clientsToGet : clientsLoaded) {
      if(clientsToGet != null) {
        return clientsToGet.getSelfUser().getId();
      }
    }
    return null;
  }

  public static long getNumberOfGuilds() {
    long numberOfGuilds = 0;

    for(JDA client : clientsLoaded) {
      numberOfGuilds += client.getGuildCache().size();
    }

    return numberOfGuilds;
  }

  public static long getNumberOfUsers() {
    long numberOfUsers = 0;

    for(JDA client : clientsLoaded) {
      numberOfUsers += client.getUserCache().size();
    }

    return numberOfUsers;
  }

  public static Emote getEmoteById(long emoteId) {
    for(JDA jda : clientsLoaded) {
      Emote emote = jda.getEmoteById(emoteId);
      if(emote != null) {
        return emote;
      }
    }
    return null;
  }

  public static TextChannel getTextChannelById(long textChannelId) {
    for(JDA jda : clientsLoaded) {
      TextChannel channel = jda.getTextChannelById(textChannelId);
      if(channel != null) {
        return channel;
      }
    }
    return null;
  }

  public static List<JDA> getJDAs(){
    return clientsLoaded;
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

  public static boolean isZoeIsBooted() {
    return zoeIsBooted;
  }
}
