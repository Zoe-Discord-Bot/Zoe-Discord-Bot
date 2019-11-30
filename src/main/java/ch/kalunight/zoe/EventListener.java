package ch.kalunight.zoe;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.discordbots.api.client.DiscordBotListAPI;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.menu.SelectionDialog;
import ch.kalunight.zoe.command.LanguageCommand;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.CleanChannelOption.CleanChannelOptionInfo;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.riotapi.CacheManager;
import ch.kalunight.zoe.service.InfoPanelRefresher;
import ch.kalunight.zoe.service.RiotApiUsageChannelRefresh;
import ch.kalunight.zoe.service.ServerChecker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.EventListenerUtil;
import ch.kalunight.zoe.util.LanguageUtil;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivityOrderEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.rithms.riot.api.RiotApiException;

public class EventListener extends ListenerAdapter {

  //Useless to translate
  private static final String WELCOME_MESSAGE = "Hello ! Thank you for adding me to your server ! "
      + "I'm here to help you to configurate your server with some "
      + "basic options. You can always do the command `>setup` or `>help` if you need help.\n\n"
      + "First, please choose your language. (Will be defined for the server, i only speak in english in private message)";
  

  private static Logger logger = LoggerFactory.getLogger(EventListener.class);

  @Override
  public void onReady(ReadyEvent event) {
    Zoe.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
    Zoe.getJda().getPresence().setActivity(Activity.playing("Booting ..."));

    setupNonInitializedGuild();

    logger.info("Loading of champions ...");
    try {
      Zoe.loadChampions();
    } catch(IOException e) {
      logger.error("Critical error with the loading of champions !", e);
      System.exit(1);
    }

    logger.info("Loading of champions finished !");

    logger.info("Loading of translations ...");
    try {
      LanguageManager.loadTranslations();
    } catch(IOException e) {
      logger.error("Critical error with the loading of translations (File issue) !", e);
      System.exit(1);
    }
    logger.info("Loading of translation finished !");
    
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
    
    logger.info("Loading of guilds ...");
    try {
      Zoe.loadDataTxt();
    } catch(IOException e) {
      logger.error("Critical error with the loading of guilds (File issue) !", e);
      System.exit(1);
    } catch(RiotApiException e) {
      logger.error("Critical error with the Riot API when loadings of guilds (Riot Api issue) !", e);
      System.exit(1);
    }

    logger.info("Loading of guilds finished !");

    logger.info("Loading of RAPI Status Channel ...");

    initRAPIStatusChannel();

    logger.info("Loading of RAPI Status Channel finished !");

    logger.info("Loading of DiscordBotList API ...");

    try {
      Zoe.setBotListApi(new DiscordBotListAPI.Builder().botId(Zoe.getJda().getSelfUser().getId()).token(Zoe.getDiscordBotListTocken()) // SET
          .build());                                                                                                                   // TOCKEN

      logger.info("Loading of DiscordBotList API finished !");
    } catch(Exception e) {
      logger.info("Discord bot list api not loaded normally ! Working of the bot not affected");
      Zoe.setBotListApi(null);
    }

    logger.info("Setup of main thread  ...");

    setupContinousRefreshThread();

    logger.info("Setup of main thread finished !");

    Zoe.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
    Zoe.getJda().getPresence().setActivity(Activity.playing("type \">help\""));
    logger.info("Booting finished !");
  }

  private void setupNonInitializedGuild() {
    for(Guild guild : Zoe.getJda().getGuilds()) {
      if(!guild.getOwnerId().equals(Zoe.getJda().getSelfUser().getId())) {
        Server server = ServerData.getServers().get(guild.getId());

        if(server == null) {
          ServerData.getServers().put(guild.getId(), new Server(guild.getIdLong(), LanguageManager.DEFAULT_LANGUAGE, new ServerConfiguration()));
        }
      }
    }
  }

  private void setupContinousRefreshThread() {
    TimerTask mainThread = new ServerChecker();
    ServerData.getServerCheckerThreadTimer().schedule(mainThread, 0);
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
      ServerData.getServers().put(event.getGuild().getId(), new Server(event.getGuild().getIdLong(), LanguageManager.DEFAULT_LANGUAGE, new ServerConfiguration()));
      ServerData.getServersIsInTreatment().put(event.getGuild().getId(), false);
      askingConfig(event.getGuild(), event.getGuild().getOwner().getUser());
    }
  }
  
  private void askingConfig(Guild guild, User owner) {
    
    Server server = ServerData.getServers().get(guild.getId());
    
    MessageChannel channel;
    
    MessageChannel channelOfGuild = CommandUtil.getFullSpeakableChannel(guild);
    
    if(channelOfGuild != null) {
      channel = channelOfGuild;
    }else {
      channel = owner.openPrivateChannel().complete();
    }
    
    channel.sendMessage(WELCOME_MESSAGE).complete();
    
    SelectionDialog.Builder builder = new SelectionDialog.Builder()
        .setTimeout(60, TimeUnit.MINUTES)
        .setColor(Color.GREEN)
        .useLooping(true)
        .setSelectedEnds("**", "**")
        .setEventWaiter(Zoe.getEventWaiter());
    
    List<String> langagesList = new ArrayList<>();
    List<String> translatedLanguageList = new ArrayList<>();
    for(String langage : LanguageManager.getListlanguages()) {
      builder.addChoices(LanguageManager.getText(langage, LanguageCommand.NATIVE_LANGUAGE_TRANSLATION_ID) 
          + " " + LanguageManager.getPourcentageTranslated(langage));
      translatedLanguageList.add(LanguageManager.getText(langage, LanguageCommand.NATIVE_LANGUAGE_TRANSLATION_ID));
      langagesList.add(langage);
    }
    
    builder.setText(LanguageUtil.getUpdateMessageAfterChangeSelectAction(LanguageManager.DEFAULT_LANGUAGE, translatedLanguageList));
    builder.setSelectionConsumer(EventListenerUtil.getSelectionDoneActionLangueSelection(langagesList, server, channel));
    builder.setCanceled(LanguageUtil.getCancelActionSelection());
    
    builder.build().display(channel);
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

    if(server != null) {

      Role optionRole = server.getConfig().getZoeRoleOption().getRole();

      if(optionRole != null && optionRole.equals(event.getRole())) {
        server.getConfig().getZoeRoleOption().setRole(null);
      }
    }
  }

  @Override
  public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
    if(event == null) {
      return;
    }
    
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    Player player = server.getPlayerByDiscordId(event.getUser().getIdLong());
    
    if(player != null) {
      server.deletePlayer(player);
    }
  }
  
  @Override
  public void onUserUpdateActivityOrder(UserUpdateActivityOrderEvent event) {
    if(event == null || event.getNewValue().isEmpty()) {
      return;
    }

    for(Activity activity : event.getNewValue()) {
      
      if(activity.isRich() && EventListenerUtil.checkIfIsGame(activity.asRichPresence()) && event.getGuild() != null) {
        Server server = ServerData.getServers().get(event.getGuild().getId());

        if(server == null) {
          return;
        }

        Player registedPlayer = null;

        for(Player player : server.getPlayers()) {
          if(player.getDiscordUser().equals(event.getUser())) {
            registedPlayer = player;
          }
        }

        if(server.getInfoChannel() != null && registedPlayer != null && !ServerData.isServerWillBeTreated(server) 
            && server.getLastRefresh().isBefore(DateTime.now().minusSeconds(30))) {

          ServerData.getServersIsInTreatment().put(event.getGuild().getId(), true);
          ServerData.getServerExecutor().execute(new InfoPanelRefresher(server, true));
          break;
        }
      }
    }
  }

  @Override
  public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
    if(event == null || event.getMessage() == null) {
      return;
    }

    Server server = ServerData.getServers().get(event.getGuild().getId());
    if(server == null) {
      return;
    }

    if(server.getConfig().getCleanChannelOption().getCleanChannelOption().equals(CleanChannelOptionInfo.DISABLE)) {
      return;
    }

    if(event.getAuthor().equals(Zoe.getJda().getSelfUser()) && event.getMessage().getContentRaw().startsWith("Info : From now on,")) {
      return;
    }

    Member member = event.getGuild().getMember(event.getAuthor());

    if(member.getUser() != Zoe.getJda().getSelfUser() && member.getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      return;
    }

    if(server.getConfig().getCleanChannelOption().getCleanChannelOption().equals(CleanChannelOptionInfo.ONLY_ZOE_COMMANDS)
        && event.getChannel().equals(server.getConfig().getCleanChannelOption().getCleanChannel())) {

      if(event.getMessage().getContentRaw().startsWith(Zoe.BOT_PREFIX) || member.getUser().equals(Zoe.getJda().getSelfUser())) {
        event.getMessage().delete().queueAfter(3, TimeUnit.SECONDS);
      }

    }else if(server.getConfig().getCleanChannelOption().getCleanChannelOption().equals(CleanChannelOptionInfo.ALL)
        && server.getConfig().getCleanChannelOption().getCleanChannel().equals(event.getChannel())) {
      event.getMessage().delete().queueAfter(3, TimeUnit.SECONDS);
    }
  }
}
