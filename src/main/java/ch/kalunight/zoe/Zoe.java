package ch.kalunight.zoe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import ch.kalunight.zoe.command.AboutCommand;
import ch.kalunight.zoe.command.RecoveryCommand;
import ch.kalunight.zoe.command.ResetCommand;
import ch.kalunight.zoe.command.ResetEmotesCommand;
import ch.kalunight.zoe.command.SetupCommand;
import ch.kalunight.zoe.command.ShutDownCommand;
import ch.kalunight.zoe.command.add.AddCommand;
import ch.kalunight.zoe.command.admin.AdminCommand;
import ch.kalunight.zoe.command.create.CreateCommand;
import ch.kalunight.zoe.command.define.DefineCommand;
import ch.kalunight.zoe.command.define.UndefineCommand;
import ch.kalunight.zoe.command.delete.DeleteCommand;
import ch.kalunight.zoe.command.remove.RemoveCommand;
import ch.kalunight.zoe.command.stats.StatsCommand;
import ch.kalunight.zoe.model.Champion;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.CustomEmote;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import ch.kalunight.zoe.model.Team;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.request.ratelimit.PriorityManagerRateLimitHandler;
import net.rithms.riot.api.request.ratelimit.PriorityRateLimit;
import net.rithms.riot.api.request.ratelimit.RateLimitHandler;
import net.rithms.riot.api.request.ratelimit.RateLimitRequestTank;
import net.rithms.riot.constant.Platform;

public class Zoe {

  public static final String BOT_PREFIX = ">";

  private static final File SAVE_TXT_FILE = new File("ressources/save.txt");

  private static List<Command> mainCommands;

  private static RiotApi riotApi;

  private static JDA jda;

  private static String discordBotListTocken = "";

  private static DiscordBotListAPI botListApi;

  private static final ConcurrentLinkedQueue<List<CustomEmote>> emotesNeedToBeUploaded = new ConcurrentLinkedQueue<>();

  private static final Logger logger = LoggerFactory.getLogger(Zoe.class);

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

    EventWaiter eventWaiter = new EventWaiter();
    
    for(Command command : getMainCommands(eventWaiter)) {
      client.addCommand(command);
    }
    
    Consumer<CommandEvent> helpCommand = getHelpCommand();

    client.setHelpConsumer(helpCommand);

    initRiotApi(riotTocken);

    try {
      jda = new JDABuilder(AccountType.BOT)//
          .setToken(discordTocken)//
          .setStatus(OnlineStatus.DO_NOT_DISTURB)//
          .addEventListener(client.build())//
          .addEventListener(eventWaiter)//
          .addEventListener(new EventListener()).build();//
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

    RateLimitHandler defaultLimite = new PriorityManagerRateLimitHandler(); // create default priority with dev api key rate limit

    config.setRateLimitHandler(defaultLimite);
    riotApi = new RiotApi(config);
  }

  private static Consumer<CommandEvent> getHelpCommand() {
    return new Consumer<CommandEvent>() {
      @Override
      public void accept(CommandEvent event) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Here is my commands :\n");

        Command setupCommand = new SetupCommand();
        stringBuilder.append("Command **" + setupCommand.getName() + "** :\n");
        stringBuilder.append("--> `>" + setupCommand.getName() + "` : " + setupCommand.getHelp() + "\n\n");

        Command aboutCommand = new AboutCommand();
        stringBuilder.append("Command **" + aboutCommand.getName() + "** :\n");
        stringBuilder.append("--> `>" + aboutCommand.getName() + "` : " + aboutCommand.getHelp() + "\n\n");
        
        Command recoveryCommand = new RecoveryCommand(null);
        stringBuilder.append("Command **" + recoveryCommand.getName() + "** :\n");
        stringBuilder.append("--> `>" + recoveryCommand.getName() + " " + recoveryCommand.getArguments() + "` : " + recoveryCommand.getHelp() + "\n\n");
        
        Command resetCommand = new ResetCommand(null);
        stringBuilder.append("Command **" + resetCommand.getName() + "** :\n");
        stringBuilder.append("--> `>" + resetCommand.getName() + "` : " + resetCommand.getHelp() + "\n\n");
        
        for(Command command : getMainCommands(null)) {
          if(!command.isHidden() && !(command instanceof PingCommand || command instanceof RecoveryCommand || command instanceof ResetCommand)) {
            stringBuilder.append("Commands **" + command.getName() + "** : \n");

            for(Command commandChild : command.getChildren()) {
              stringBuilder.append("--> `>" + command.getName() + " " + commandChild.getName() + " " + commandChild.getArguments() + "` : "
                  + commandChild.getHelp() + "\n");
            }
            stringBuilder.append(" \n");
          }
        }

        stringBuilder.append("For additional help, you can join our official server : https://discord.gg/whc5PrC");

        PrivateChannel privateChannel = event.getAuthor().openPrivateChannel().complete();
        privateChannel.sendMessage(stringBuilder.toString()).queue();
        event.reply("I send you all the commands in a private message.");
      }
    };
  }

  public static List<Command> getMainCommands(EventWaiter eventWaiter) {
    if(mainCommands != null) {
      return mainCommands;
    }
    List<Command> commands = new ArrayList<>();

    // Admin commands
    commands.add(new ShutDownCommand());
    commands.add(new ResetEmotesCommand());
    commands.add(new PingCommand());
    commands.add(new AdminCommand());

    // Basic commands
    commands.add(new AboutCommand());
    commands.add(new SetupCommand());
    commands.add(new CreateCommand());
    commands.add(new DeleteCommand());
    commands.add(new DefineCommand());
    commands.add(new UndefineCommand());
    commands.add(new AddCommand());
    commands.add(new RemoveCommand());
    commands.add(new StatsCommand());
    commands.add(new RecoveryCommand(eventWaiter));
    commands.add(new ResetCommand(eventWaiter));
    
    mainCommands = commands;

    return commands;
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

  public static Player searchPlayerWithDiscordId(List<Player> players, String discordId) {
    for(Player player : players) {
      if(player.getDiscordUser().getId().equals(discordId)) {
        return player;
      }
    }
    return null;
  }

  public static synchronized void saveDataTxt() throws FileNotFoundException, UnsupportedEncodingException {
    final StringBuilder strMainBuilder = new StringBuilder();

    final Map<String, Server> servers = ServerData.getServers();
    final List<Guild> guilds = Zoe.getJda().getGuilds();

    for(Guild guild : guilds) {
      try {
        if(guild.getOwnerId().equals(Zoe.getJda().getSelfUser().getId())) {
          continue;
        }
        Server server = servers.get(guild.getId());
        StringBuilder strBuilder = new StringBuilder();
        
        if(server != null) {
          strBuilder.append("--server\n");
          strBuilder.append(guild.getId() + "\n");
          strBuilder.append(server.getLangage().toString() + "\n");

          strBuilder.append(server.getPlayers().size() + "\n");

          for(Player player : server.getPlayers()) {
            strBuilder.append(player.getDiscordUser().getId() + "\n");
            strBuilder.append(player.getSummoner().getId() + "\n");
            strBuilder.append(player.getRegion().getName() + "\n");
            strBuilder.append(player.isMentionnable() + "\n");
          }

          strBuilder.append(server.getTeams().size() + "\n");

          for(Team team : server.getTeams()) {
            strBuilder.append(team.getName() + "\n");

            strBuilder.append(team.getPlayers().size() + "\n");

            for(Player player : team.getPlayers()) {
              strBuilder.append(player.getDiscordUser().getId() + "\n");
            }
          }

          if(server.getInfoChannel() != null) {
            strBuilder.append(server.getInfoChannel().getId() + "\n");
          } else {
            strBuilder.append("-1\n");
          }

          if(server.getControlePannel() != null) {
            strBuilder.append(server.getControlePannel().getInfoPanel().size() + "\n");
            for(Message message : server.getControlePannel().getInfoPanel()) {
              strBuilder.append(message.getId() + "\n");
            }
          }else {
            strBuilder.append("0\n");
          }
        }
        strMainBuilder.append(strBuilder.toString());
      }catch(Exception e) {
        logger.warn("A guild occured a error, it hasn't been saved.", e);
      }
    }

    try(PrintWriter writer = new PrintWriter(SAVE_TXT_FILE, "UTF-8");) {
      writer.write(strMainBuilder.toString());
    }
  }

  public static void loadDataTxt() throws IOException, RiotApiException {

    try(final BufferedReader reader = new BufferedReader(new FileReader(SAVE_TXT_FILE));) {
      String line;

      while((line = reader.readLine()) != null) {

        if(line.equalsIgnoreCase("--server")) {
          final String guildId = reader.readLine();
          final Guild guild = jda.getGuildById(guildId);
          if(guild == null) {
            continue;
          }
          SpellingLangage langage = SpellingLangage.valueOf(reader.readLine());
          if(langage == null) {
            langage = SpellingLangage.EN;
          }

          final Server server = new Server(guild, langage);

          final Long nbrPlayers = Long.parseLong(reader.readLine());

          final List<Player> players = createPlayers(reader, nbrPlayers);

          final Long nbrTeams = Long.parseLong(reader.readLine());

          final List<Team> teams = createTeams(reader, players, nbrTeams);

          final TextChannel pannel = guild.getTextChannelById(reader.readLine());

          setInfoPannel(guild, server, pannel);

          int nbrMessageControlPannel = Integer.parseInt(reader.readLine());
          ControlPannel controlPannel = getControlePannel(reader, server, nbrMessageControlPannel);

          server.setPlayers(players);
          server.setTeams(teams);
          server.setControlePannel(controlPannel);
          ServerData.getServers().put(guildId, server);
          ServerData.getServersIsInTreatment().put(guildId, false);
        }
      }
    }
  }

  private static ControlPannel getControlePannel(final BufferedReader reader, final Server server, int nbrMessageControlPannel)
      throws IOException {
    ControlPannel controlPannel = new ControlPannel();

    for(int i = 0; i < nbrMessageControlPannel; i++) {
      String messageId = reader.readLine();

      if(server.getInfoChannel() != null) {
        try {
          Message message = server.getInfoChannel().getMessageById(messageId).complete();
          controlPannel.getInfoPanel().add(message);
        } catch(ErrorResponseException e) {
          logger.debug("The message got delete : {}", e.getMessage());
        }
      }
    }
    return controlPannel;
  }

  private static void setInfoPannel(final Guild guild, final Server server, final TextChannel pannel) {
    if(pannel != null) {
      server.setInfoChannel(pannel);
    }
  }

  private static List<Team> createTeams(BufferedReader reader, List<Player> players, Long nbrTeams) throws IOException {
    List<Team> teams = new ArrayList<>();

    for(Long i = 0L; i < nbrTeams; i++) {
      String teamName = reader.readLine();
      int nbrPlayersInTeam = Integer.parseInt(reader.readLine());

      List<Player> listPlayers = new ArrayList<>();

      for(int j = 0; j < nbrPlayersInTeam; j++) {
        String discordId = reader.readLine();

        Player player = searchPlayerWithDiscordId(players, discordId);

        if(player != null) {
          listPlayers.add(player);
        }
      }
      teams.add(new Team(teamName, listPlayers));
    }
    return teams;
  }

  private static List<Player> createPlayers(BufferedReader reader, Long nbrPlayers) throws IOException, RiotApiException {
    List<Player> players = new ArrayList<>();

    for(Long i = 0L; i < nbrPlayers; i++) {
      String discordId = reader.readLine();
      String summonerId = reader.readLine();
      String summonerRegion = reader.readLine();
      String mentionableString = reader.readLine();

      User user = jda.getUserById(discordId);
      Platform region = Platform.getPlatformByName(summonerRegion);
      Summoner summoner = riotApi.getSummoner(region, summonerId);
      boolean mentionable = Boolean.getBoolean(mentionableString);

      players.add(new Player(user, summoner, region, mentionable));
    }
    return players;
  }

  public static RiotApi getRiotApi() {
    return riotApi;
  }

  public static void setRiotApi(RiotApi riotApi) {
    Zoe.riotApi = riotApi;
  }

  public static JDA getJda() {
    return jda;
  }

  public static void setJda(JDA jda) {
    Zoe.jda = jda;
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

  public static void setDiscordBotListTocken(String discordBotListTocken) {
    Zoe.discordBotListTocken = discordBotListTocken;
  }
}
