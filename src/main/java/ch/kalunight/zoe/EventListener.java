package ch.kalunight.zoe;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import org.discordbots.api.client.DiscordBotListAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.model.static_data.SpellingLangage;
import ch.kalunight.zoe.service.GameChecker;
import ch.kalunight.zoe.service.RiotApiUsageChannelRefresh;
import ch.kalunight.zoe.util.EventListenerUtil;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.ActivityFlag;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.RichPresence;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.events.user.update.UserUpdateGameEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.constant.CallPriority;

public class EventListener extends ListenerAdapter {

  private static final int WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS = 30000;

  private static final String WELCOME_MESSAGE = "Hi! Thank you for adding me! To get help on my configuration type the command `>setup`. "
      + "If you want to see all commands i have, type >`help`";

  private static Logger logger = LoggerFactory.getLogger(EventListener.class);

  @Override
  public void onReady(ReadyEvent event) {
    Zoe.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
    Zoe.getJda().getPresence().setGame(Game.playing("Booting ..."));

    setupNonInitializedGuild();

    logger.info("Chargements des champions ...");
    try {
      Zoe.loadChampions();
    } catch(IOException e1) {
      logger.error("Erreur lors du chargement des champions !");
      System.exit(1);
    }

    logger.info("Chargements des champions terminé !");

    logger.info("Chargements des emotes ...");
    try {
      EventListenerUtil.loadCustomEmotes();
      logger.info("Chargements des emotes terminé !");
    } catch(IOException e) {
      logger.error("Erreur lors du chargment des emotes : {}", e.getMessage());
    }

    logger.info("Chargement des sauvegardes détaillés ...");
    try {
      Zoe.loadDataTxt();
    } catch(IOException e) {
      logger.error(e.getMessage());
      logger.info("Une erreur est survenu lors du chargement des sauvegardes détaillés !");
      System.exit(1);
    } catch(RiotApiException e) {
      logger.error(e.getMessage());
      logger.info("Une erreur venant de l'api Riot est survenu lors du chargement des sauvegardes détaillés !");
      System.exit(1);
    }

    logger.info("Chargement des sauvegardes détaillés terminé !");
    
    logger.info("Loading of RAPI Status Channel ...");
    
    initRAPIStatusChannel();
    
    logger.info("Loading of RAPI Status Channel finished !");

    logger.info("Loading of DiscordBotList API ...");

    try {
      Zoe.setBotListApi(new DiscordBotListAPI.Builder().botId(Zoe.getJda().getSelfUser().getId()).token(Zoe.getDiscordBotListTocken()) // SET
                                                                                                                                       // TOCKEN
          .build());
      logger.info("Loading of DiscordBotList API finished !");
    } catch(Exception e) {
      logger.info("Discord bot list api not loaded normally ! Working of the bot not affected");
      Zoe.setBotListApi(null);
    }

    logger.info("Démarrage des tâches continue...");

    setupContinousRefreshThread();

    logger.info("Démarrage des tâches continues terminés !");

    Zoe.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
    Zoe.getJda().getPresence().setGame(Game.playing("type \">help\""));
    logger.info("Démarrage terminés !");
  }

  private void setupNonInitializedGuild() {
    for(Guild guild : Zoe.getJda().getGuilds()) {
      if(!guild.getOwnerId().equals(Zoe.getJda().getSelfUser().getId())) {
        Server server = ServerData.getServers().get(guild.getId());

        if(server == null) {
          ServerData.getServers().put(guild.getId(), new Server(guild, SpellingLangage.EN, new ServerConfiguration()));
        }
      }
    }
  }

  private void setupContinousRefreshThread() {
    TimerTask mainThread = new GameChecker();
    ServerData.getMainThreadTimer().schedule(mainThread, 0, WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS);

    Runnable gameChecker = new GameChecker();
    Thread thread = new Thread(gameChecker, "Game-Checker-Thread");
    thread.start();
  }

  private void initRAPIStatusChannel() {
    try(final BufferedReader reader = new BufferedReader(new FileReader(Zoe.RAPI_SAVE_TXT_FILE));) {
      String line;

      List<String> args = new ArrayList<>();
      
      while((line = reader.readLine()) != null) {
        args.add(line);
      }
      
      if(args.size() == 2) {
        Guild guild = Zoe.getJda().getGuildById(args.get(0));
        if(guild != null) {
          TextChannel rapiStatusChannel = guild.getTextChannelById(args.get(1));
          if(rapiStatusChannel != null) {
            RiotApiUsageChannelRefresh.setRapiInfoChannel(rapiStatusChannel);
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
  
  @Override
  public void onGuildJoin(GuildJoinEvent event) {

    if(!event.getGuild().getOwner().getUser().getId().equals(Zoe.getJda().getSelfUser().getId())) {
      ServerData.getServers().put(event.getGuild().getId(), new Server(event.getGuild(), SpellingLangage.EN, new ServerConfiguration()));
      ServerData.getServersIsInTreatment().put(event.getGuild().getId(), false);
      CommandUtil.sendMessageInGuildOrAtOwner(event.getGuild(), WELCOME_MESSAGE);
    }
  }

  @Override
  public void onTextChannelDelete(TextChannelDeleteEvent event) {
    Server server = ServerData.getServers().get(event.getGuild().getId());
    if(server.getInfoChannel() != null && server.getInfoChannel().getId().equals(event.getChannel().getId())) {
      server.setControlePannel(new ControlPannel());
      server.setInfoChannel(null);
    }
  }
  
  @Override
  public void onRoleDelete(RoleDeleteEvent event) {
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    Role optionRole = server.getConfig().getZoeRoleOption().getRole();
    
    if(optionRole != null && optionRole.equals(event.getRole())) {
      server.getConfig().getZoeRoleOption().setRole(null);
    }
  }
  
  @Override
  public void onUserUpdateGame(UserUpdateGameEvent event) {
    if(event.getGuild() != null) {
      Server server = ServerData.getServers().get(event.getGuild().getId());
      
      Player registedPlayer = null;
      
      for(Player player : server.getPlayers()) {
        if(player.getDiscordUser().equals(event.getUser())) {
          registedPlayer = player;
        }
      }
      
      if(server.getInfoChannel() != null && registedPlayer != null && event.getNewGame().isRich()) {
        RichPresence richPresenceGame = event.getNewGame().asRichPresence();
        if(richPresenceGame.getName() != null && richPresenceGame.getName().equals("League of Legends")) {
          boolean inGame = false;
          for(ActivityFlag flag : richPresenceGame.getFlagSet()) {
            if(flag.equals(ActivityFlag.PLAY)) {
              inGame = true;
            }
          }
          
          if(inGame) {
            
            
            registedPlayer.refreshAllLeagueAccounts(CallPriority.NORMAL);
            
            ArrayList<InfoCard> cards = new ArrayList<>();
            for(LeagueAccount leagueAccount : registedPlayer.getLeagueAccountsInGame()) {
                
              CurrentGameInfo gameInfo = leagueAccount.getCurrentGameInfo();
              
              List<Player> playersInTheGame = InfoPanelRefresherUtil.checkIfOthersPlayersIsKnowInTheMatch(gameInfo, server);
              
              InfoCard card = null;
              
              if(playersInTheGame.size() == 1) {
                Player player = playersInTheGame.get(0);
                MessageEmbed messageCard = MessageBuilderRequest.createInfoCard1summoner(player.getDiscordUser(), leagueAccount.getSummoner(),
                    gameInfo, leagueAccount.getRegion());
                if(messageCard != null) {
                  card = new InfoCard(playersInTheGame, messageCard, gameInfo);
                }
              } else if(playersInTheGame.size() > 1) {
                MessageEmbed messageCard =
                    MessageBuilderRequest.createInfoCardsMultipleSummoner(playersInTheGame, gameInfo, leagueAccount.getRegion());

                if(messageCard != null) {
                  card = new InfoCard(playersInTheGame, messageCard, gameInfo);
                }
              }
              
              server.getCurrentGamesIdAlreadySended().add(gameInfo.getGameId());
              
              if(card != null) {
                List<Player> players = card.getPlayers();

                StringBuilder title = new StringBuilder();
                title.append("Info on the game of");

                List<String> playersName = NameConversion.getListNameOfPlayers(players);

                Set<Player> cardPlayersNotTwice = new HashSet<>();
                for(Player playerToCheck : card.getPlayers()) {
                  cardPlayersNotTwice.add(playerToCheck);
                }

                for(int j = 0; j < cardPlayersNotTwice.size(); j++) {
                  if(j == 0) {
                    title.append(" " + playersName.get(j));
                  } else if(j + 1 == playersName.size()) {
                    title.append(" and of " + playersName.get(j));
                  } else if(j + 2 == playersName.size()) {
                    title.append(" " + playersName.get(j));
                  } else {
                    title.append(" " + playersName.get(j) + ",");
                  }
                }

                card.setTitle(server.getInfoChannel().sendMessage(title.toString()).complete());
                card.setMessage(server.getInfoChannel().sendMessage(card.getCard()).complete());

                cards.add(card);
              }
            }
            
            server.getControlePannel().getInfoCards().addAll(cards);
            
          }
        }
      }
    }
  }
  
  
  
}
