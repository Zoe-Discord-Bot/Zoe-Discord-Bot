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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import ch.kalunight.zoe.command.ShutDownCommand;
import ch.kalunight.zoe.command.create.CreateCommand;
import ch.kalunight.zoe.command.delete.DeleteCommand;
import ch.kalunight.zoe.model.Champion;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.CustomEmote;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import ch.kalunight.zoe.model.Team;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.SleeperRateLimitHandler;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.request.ratelimit.RateLimitHandler;
import net.rithms.riot.constant.Platform;

public class Zoe {

  private static final File SAVE_TXT_FILE = new File("ressources/save.txt");

  private static RiotApi riotApi;

  private static JDA jda;

  private static final ConcurrentLinkedQueue<List<CustomEmote>> emotesNeedToBeUploaded = new ConcurrentLinkedQueue<>();

  private static final Logger logger = LoggerFactory.getLogger(Zoe.class);

  public static void main(String[] args) {

    System.setProperty("logback.configurationFile", "logback.xml");

    String discordTocken = args[0];
    String riotTocken = args[1];

    CommandClientBuilder client = new CommandClientBuilder();

    client.setPrefix(">");

    client.setOwnerId(args[2]);
    
    client.addCommands(new ShutDownCommand(), new CreateCommand(), new DeleteCommand(), new PingCommand());

    ApiConfig config = new ApiConfig().setKey(riotTocken);

    RateLimitHandler defaultLimite = new SleeperRateLimitHandler();

    config.setRateLimitHandler(defaultLimite);
    riotApi = new RiotApi(config);

    try {
      jda = new JDABuilder(AccountType.BOT)//
          .setToken(discordTocken)//
          .setStatus(OnlineStatus.DO_NOT_DISTURB)//
          .addEventListener(client.build())//
          .addEventListener(new EventListener())
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

  public static Player searchPlayerWithDiscordId(List<Player> players, String discordId) {
    for(Player player : players) {
      if(player.getDiscordUser().getId().equals(discordId)) {
        return player;
      }
    }
    return null;
  }

  public static synchronized void saveDataTxt() throws FileNotFoundException, UnsupportedEncodingException {
    final StringBuilder strBuilder = new StringBuilder();
    
    final Map<String, Server> servers = ServerData.getServers();
    final List<Guild> guilds = Zoe.getJda().getGuilds();
    
    for(Guild guild : guilds) {
      if(guild.getOwnerId().equals(Zoe.getJda().getSelfUser().getId())) {
        continue;
      }
      Server server = servers.get(guild.getId());
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
        }else {
          strBuilder.append("-1\n");
        }
        
        strBuilder.append(server.getControlePannel().getInfoPanel().size() + "\n");
        
        for(Message message : server.getControlePannel().getInfoPanel()) {
          strBuilder.append(message.getId() + "\n");
        }
      }
    }

    try (PrintWriter writer = new PrintWriter(SAVE_TXT_FILE, "UTF-8");){
      writer.write(strBuilder.toString());
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
        controlPannel.getInfoPanel().add(server.getInfoChannel().getMessageById(messageId).complete());
      }
    }
    return controlPannel;
  }

  private static void setInfoPannel(final Guild guild, final Server server, final TextChannel pannel) {
    if(pannel != null) {
      server.setInfoChannel(pannel);
    }else {
      sendInfoMessageToAdminAboutTheInitializePhase(guild);
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

  private static void sendInfoMessageToAdminAboutTheInitializePhase(Guild guild) {
    List<TextChannel> textChannels = guild.getTextChannels();
    
    boolean messageSended = false;
    for(TextChannel textChannel : textChannels) {
      if(textChannel.canTalk()) {
        textChannel.sendMessage("Hey ! You don't have create yet a info channel with players informations (and i want to do my work :p), "
            + "admins can create one with the command `>createInfoChannel *nameOfTheChannel*` or define a channel already existent with "
            + "`>defineInfoChannel *#nameOfTextChannel*`.\nHave a good day !").queue();
        messageSended = true;
        break;
      }
    }
    
    if(!messageSended) {
      PrivateChannel privateChannel = guild.getOwner().getUser().openPrivateChannel().complete();
      privateChannel.sendMessage("Hey ! You don't have create yet a info channel with players informations (and i want to do my work :p), "
            + "admins can create one with the command `>createInfoChannel *nameOfTheChannel*` or define a channel already existent with "
            + "`>defineInfoChannel *#nameOfTextChannel*`.\nHave a good day !").queue();
    }
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
}
