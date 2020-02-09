package ch.kalunight.zoe;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.discordbots.api.client.DiscordBotListAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.command.LanguageCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.CleanChannelOption.CleanChannelOptionInfo;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.RankHistoryChannelRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.repositories.ServerStatusRepository;
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
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventListener extends ListenerAdapter {
  
  //Useless to translate
  private static final String WELCOME_MESSAGE = "Hello ! Thank you for adding me to your server ! "
      + "I'm here to help you to configurate your server with some "
      + "basic options. You can always do the command `>setup` or `>help` if you need help.\n\n"
      + "First, please choose your language. (Will be defined for the server, i only speak in english in private message)";

  private static final Logger logger = LoggerFactory.getLogger(EventListener.class);

  @Override
  public void onReady(ReadyEvent event) {
    Zoe.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
    Zoe.getJda().getPresence().setActivity(Activity.playing("Booting ..."));

    logger.info("Setup non initialized Guild ...");
    try {
      setupNonInitializedGuild();
    } catch(SQLException e) {
      logger.error("Issue when setup non initialized Guild !", e);
      System.exit(1);
    }
    logger.info("Setup non initialized Guild Done !");

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

  private void setupNonInitializedGuild() throws SQLException {
    for(Guild guild : Zoe.getJda().getGuilds()) {
      if(!guild.getOwnerId().equals(Zoe.getJda().getSelfUser().getId()) && !ServerRepository.checkServerExist(guild.getIdLong())) {
        ServerRepository.createNewServer(guild.getIdLong(), LanguageManager.DEFAULT_LANGUAGE);
      }
    }
    ServerStatusRepository.updateAllServerInTreatment(false);
  }

  private void setupContinousRefreshThread() {
    TimerTask mainThread = new ServerChecker();
    ServerData.getServerCheckerThreadTimer().schedule(mainThread, 10000);
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

  @Override
  public void onGuildJoin(GuildJoinEvent event) {
    try {
      if(!event.getGuild().getOwner().getUser().getId().equals(Zoe.getJda().getSelfUser().getId())) {
        DTO.Server server = ServerRepository.getServer(event.getGuild().getIdLong());
        if(server == null) {
          ServerRepository.createNewServer(event.getGuild().getIdLong(), LanguageManager.DEFAULT_LANGUAGE);
          askingConfig(event.getGuild(), event.getGuild().getOwner().getUser());
        }else {
          CommandUtil.getFullSpeakableChannel(
              event.getGuild()).sendMessage(LanguageManager.getText(server.serv_language, "guildJoinHiAgain")).queue();
        }
      }
    }catch(SQLException e) {
      logger.error("SQL error when joining a guild !", e);
    }catch(Exception e) {
      logger.error("Unknown error when joining a guild !", e);
    }
  }

  private void askingConfig(Guild guild, User owner) throws SQLException {

    DTO.Server server = ServerRepository.getServer(guild.getIdLong());

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
    try {
      DTO.InfoChannel infochannel = InfoChannelRepository.getInfoChannel(event.getGuild().getIdLong());
      if(infochannel != null && infochannel.infochannel_channelid == event.getChannel().getIdLong()) {
        InfoChannelRepository.deleteInfoChannel(ServerRepository.getServer(event.getGuild().getIdLong()));
      }
      
      DTO.RankHistoryChannel rankChannel = RankHistoryChannelRepository.getRankHistoryChannel(event.getGuild().getIdLong());
      if(rankChannel != null && rankChannel.rhChannel_channelId == event.getChannel().getIdLong()) {
        RankHistoryChannelRepository.deleteRankHistoryChannel(rankChannel.rhChannel_id);
      }
    }catch(SQLException e) {
      logger.error("Issue with db when reacting to the textChannelDelete Event.", e);
    }
  }

  @Override
  public void onRoleDelete(RoleDeleteEvent event) {
    try {
      ConfigRepository.getServerConfiguration(event.getGuild().getIdLong());
    } catch(SQLException e) {
      logger.error("Issue with db when reacting to the RoleDeleteEvent event.", e);
    }
  }

  @Override
  public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
    if(event == null) {
      return;
    }

    try {
      DTO.Player player = PlayerRepository.getPlayer(event.getGuild().getIdLong(), event.getUser().getIdLong());

      if(player != null) {
        PlayerRepository.deletePlayer(player.player_id, event.getGuild().getIdLong());
      }
    }catch(SQLException e) {
      logger.error("Issue with db when reacting to the GuildMemberLeaveEvent event.", e);
    }
  }

  @Override
  public void onUserActivityStart(UserActivityStartEvent event) {
    try {
      if(event == null || event.getNewActivity() == null) {
        return;
      }

      Activity activity = event.getNewActivity();
      
        if(activity.isRich() && EventListenerUtil.checkIfIsGame(activity.asRichPresence()) && event.getGuild() != null) {
          DTO.Server server = ServerRepository.getServer(event.getGuild().getIdLong());

          if(server == null) {
            return;
          }

          DTO.Player registedPlayer = PlayerRepository.getPlayer(event.getGuild().getIdLong(), event.getUser().getIdLong());
          DTO.InfoChannel infochannel = InfoChannelRepository.getInfoChannel(event.getGuild().getIdLong());

          if(infochannel != null && registedPlayer != null && !ServerData.isServerWillBeTreated(server)
              && server.serv_lastRefresh.isBefore(LocalDateTime.now().minusSeconds(5))) {

            ServerData.getServersIsInTreatment().put(event.getGuild().getId(), true);
            ServerRepository.updateTimeStamp(server.serv_guildId, LocalDateTime.now());
            ServerData.getServerExecutor().execute(new InfoPanelRefresher(server, true));
          }
        }
    }catch(SQLException e) {
      logger.error("SQL Error when treating discord status update event !", e);
    }catch(Exception e) {
      logger.error("Unknown Error when treating discord status update event !", e);
    }
  }

  @Override
  public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
    if(event == null || event.getMessage() == null) {
      return;
    }

    ServerConfiguration config;
    try {
      config = ConfigRepository.getServerConfiguration(event.getGuild().getIdLong());
    } catch(SQLException e) {
      logger.error("SQL Error when treating message receive !", e);
      return;
    }

    if(config.getCleanChannelOption().getCleanChannelOption().equals(CleanChannelOptionInfo.DISABLE)) {
      return;
    }

    if(event.getAuthor().equals(Zoe.getJda().getSelfUser()) && event.getMessage().getContentRaw().startsWith("Info : From now on,")) {
      return;
    }

    Member member = event.getGuild().getMember(event.getAuthor());

    if(member.getUser() != Zoe.getJda().getSelfUser() && member.getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      return;
    }

    if(config.getCleanChannelOption().getCleanChannelOption().equals(CleanChannelOptionInfo.ONLY_ZOE_COMMANDS)
        && event.getChannel().equals(config.getCleanChannelOption().getCleanChannel())) {

      if(event.getMessage().getContentRaw().startsWith(Zoe.BOT_PREFIX) || member.getUser().equals(Zoe.getJda().getSelfUser())) {
        event.getMessage().delete().queueAfter(3, TimeUnit.SECONDS);
      }

    }else if(config.getCleanChannelOption().getCleanChannelOption().equals(CleanChannelOptionInfo.ALL)
        && config.getCleanChannelOption().getCleanChannel().equals(event.getChannel())) {
      event.getMessage().delete().queueAfter(3, TimeUnit.SECONDS);
    }
  }
}
