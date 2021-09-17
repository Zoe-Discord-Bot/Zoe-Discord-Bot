package ch.kalunight.zoe.command.create;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.ClashChannelData;
import ch.kalunight.zoe.model.dto.ClashStatus;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.BannedAccount;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.repositories.BannedAccountRepository;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.service.clashchannel.TreatClashChannel;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.PaginatorUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import ch.kalunight.zoe.util.TimeZoneUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import no.stelar7.api.r4j.basic.exceptions.APIHTTPErrorReason;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.impl.lol.builders.summoner.SummonerBuilder;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class CreateClashChannelRunnable {

  private static final Logger logger = LoggerFactory.getLogger(CreateClashChannelRunnable.class);
  
  private static class ClashCreationData {
    private String channelName;
    private ZoePlatform platform;
    private Summoner summoner;
  }

  private CreateClashChannelRunnable() {
    // hide default public constructor
  }
  
  public static void executeCommand(Server server, String nameChannel, EventWaiter waiter,
      Member author, Message toEdit, TextChannel channel, InteractionHook hook, boolean forceRefresh) throws SQLException {

    if(nameChannel == null || nameChannel.equals("")) {
      String message = LanguageManager.getText(server.getLanguage(), "nameOfInfochannelNeeded");
      if(toEdit != null) {
        toEdit.editMessage(message).queue();
      }else {
        hook.editOriginal(message).queue();
      }
      return;
    }

    if(nameChannel.length() > 100) {
      String message = LanguageManager.getText(server.getLanguage(), "nameOfTheInfoChannelNeedToBeLess100Characters");
      if(toEdit != null) {
        toEdit.editMessage(message).queue();
      }else {
        hook.editOriginal(message).queue();
      }
      return;
    }

    List<DTO.ClashChannel> dbClashChannels = ClashChannelRepository.getClashChannels(server.serv_guildId);

    for(DTO.ClashChannel dbClashChannel : dbClashChannels) {
      if(dbClashChannel != null && dbClashChannel.clashChannel_channelId != 0) {

        TextChannel clashChannel = channel.getGuild().getTextChannelById(dbClashChannel.clashChannel_channelId);
        if(clashChannel == null) {
          ClashChannelRepository.deleteClashChannel(dbClashChannel.clashChannel_id);
        }
      }
    }

    ClashCreationData creationData = new ClashCreationData();

    creationData.channelName = nameChannel;

    String message = LanguageManager.getText(server.getLanguage(), "createClashChannelAskLeagueAccount");

    if(toEdit != null) {
      toEdit.editMessage(message).queue();
    }else {
      hook.editOriginal(message).queue();
    }
    
    waitForALeagueAccount(author, channel, waiter, server, creationData, forceRefresh);
  }

  private static void waitForALeagueAccount(Member member, TextChannel channel, EventWaiter waiter, DTO.Server server, ClashCreationData creationData, boolean forceRefresh) {
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(member.getUser()) && e.getChannel().getId().equals(channel.getId()),
        e -> threatLeagueAccountSelection(e, server, member, channel, waiter, creationData, forceRefresh), 3, TimeUnit.MINUTES,
        () -> cancelCreationOfClashChannel(channel, server));
  }

  private static void threatLeagueAccountSelection(MessageReceivedEvent message, Server server, Member member,
      TextChannel channel, EventWaiter waiter, ClashCreationData creationData, boolean forceRefresh) {

    if(message.getMessage().getContentRaw().equalsIgnoreCase("Stop")) {
      cancelCreationOfClashChannel(message.getTextChannel(), server);
      return;
    }

    List<String> listArgs = CreatePlayerCommandRunnable.getParameterInParenteses(message.getMessage().getContentRaw());
    if(listArgs.size() != 2) {
      channel.sendMessage(LanguageManager.getText(server.getLanguage(), "createClashMalformedLeagueAccount")).queue();
      waitForALeagueAccount(member, channel, waiter, server, creationData, forceRefresh);
      return;
    }

    String regionName = listArgs.get(0);
    String summonerName = listArgs.get(1);


    ZoePlatform region = CreatePlayerCommandRunnable.getPlatform(regionName);
    if(region == null) {
      channel.sendMessage(LanguageManager.getText(server.getLanguage(), "createClashChannelRegionTagInvalid")).queue();
      waitForALeagueAccount(member, channel, waiter, server, creationData, forceRefresh);
      return;
    }

    Message loadingMessage = channel.sendMessage(LanguageManager.getText(server.getLanguage(), "loadingSummoner")).complete();
    try {
      Summoner summoner = new SummonerBuilder().withPlatform(region.getLeagueShard()).withName(summonerName).get();

      if(summoner != null) {

        List<DTO.ClashChannel> clashChannels = ClashChannelRepository.getClashChannels(server.serv_guildId);

        for(DTO.ClashChannel clashChannel : clashChannels) {
          if(clashChannel.clashChannel_data.getSelectedSummonerId().equals(summoner.getSummonerId())
              && clashChannel.clashChannel_data.getSelectedPlatform().equals(region)) {
            loadingMessage.editMessage(LanguageManager.getText(server.getLanguage(), "createClashChannelAlreadyCreatedForThisSummoner")).queue();
            waitForALeagueAccount(member, channel, waiter, server, creationData, forceRefresh);
            return;
          }
        }

        BannedAccount bannedAccount = BannedAccountRepository.getBannedAccount(summoner.getSummonerId(), region);
        if(bannedAccount == null) {

          loadingMessage.editMessage(String.format(LanguageManager.getText(server.getLanguage(), "createClashChannelLeagueAccountSelected"), region.getShowableName(), summoner.getName())).queue();

          List<String> timeZoneIds = getTimeZoneIds();

          sendListTimeZone(member, channel, waiter, server, timeZoneIds);

          creationData.summoner = summoner;
          creationData.platform = region;

          waitForATimeZoneSelection(member, channel, waiter, server, timeZoneIds, creationData, forceRefresh);
        }else {
          loadingMessage.editMessage(LanguageManager.getText(server.getLanguage(), "accountCantBeAddedOwnerChoice")).queue();
        }
      }else {
        loadingMessage.editMessage(String.format(LanguageManager.getText(server.getLanguage(), "createClashChannelSummonerNotFound"), summonerName, region.toString())).queue();
      }
    } catch (SQLException e) {
      logger.error("SQLException with league account selection in create Clash Command.", e);
      loadingMessage.editMessage(LanguageManager.getText(server.getLanguage(), "deleteLeaderboardErrorDatabase")).queue();
    } catch (APIResponseException e) {
      RiotApiUtil.handleRiotApi(loadingMessage, e, server.getLanguage());

      if(e.getReason() == APIHTTPErrorReason.ERROR_404 || e.getReason() == APIHTTPErrorReason.ERROR_500) {
        waitForALeagueAccount(member, channel, waiter, server, creationData, forceRefresh);
      }
    }
  }

  private static void sendListTimeZone(Member member, TextChannel channel, EventWaiter waiter, Server server, List<String> timeZoneIds) {
    Paginator.Builder pbuilder = new Paginator.Builder()
        .setColumns(1)
        .setItemsPerPage(30)
        .showPageNumbers(true)
        .waitOnSinglePage(false)
        .useNumberedItems(true)
        .setFinalAction(m -> {
          try {
            m.clearReactions().queue();
          } catch(PermissionException ex) {
            m.delete().queue();
          }
        })
        .setEventWaiter(waiter)
        .setUsers(member.getUser())
        .setColor(Color.GREEN)
        .setText(PaginatorUtil.getPaginationTranslatedPage(server.getLanguage()))
        .setText(LanguageManager.getText(server.getLanguage(), "paginationTimeZoneList"))
        .setTimeout(3, TimeUnit.MINUTES);

    for(String timeZoneId : timeZoneIds) {
      pbuilder.addItems(TimeZoneUtil.displayTimeZone(TimeZone.getTimeZone(timeZoneId)));
    }

    pbuilder.build().display(channel);
  }

  private static List<String> getTimeZoneIds() {
    return Arrays.asList(TimeZone.getAvailableIDs());
  }

  private static void threatTimeZoneSelection(MessageReceivedEvent message, Server server, Member member, TextChannel channel, EventWaiter waiter,
      List<String> timeZoneIds, ClashCreationData creationData, boolean forceRefresh) {
    if(message.getMessage().getContentRaw().equalsIgnoreCase("Stop")) {
      cancelCreationOfClashChannel(message.getTextChannel(), server);
      return;
    }

    try {
      int timeZoneNumber = Integer.parseInt(message.getMessage().getContentRaw());
      if(timeZoneNumber >= 1 && timeZoneNumber < timeZoneIds.size()) {
        String timeZoneSelected = timeZoneIds.get(timeZoneNumber - 1);

        selectTimeZoneWithName(message, server, timeZoneIds, timeZoneSelected, member, channel, waiter, creationData, forceRefresh);
      }else {
        message.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(),
            "createClashChannelTournamentBadTimeZoneSelection")).queue();
        waitForATimeZoneSelection(member, channel, waiter, server, timeZoneIds, creationData, forceRefresh);
      }
    }catch(NumberFormatException e) {
      selectTimeZoneWithName(message, server, timeZoneIds, message.getMessage().getContentRaw(), member, channel, waiter, creationData, forceRefresh);
    }
  }

  private static void selectTimeZoneWithName(MessageReceivedEvent message, Server server, List<String> timeZoneIds, String receivedTimeZone,
      Member member, TextChannel channel, EventWaiter waiter, ClashCreationData creationData, boolean forceRefresh) {
    String selectedTimeZone = null;
    for(String timeZoneToCheck : timeZoneIds) {
      if(timeZoneToCheck.equalsIgnoreCase(receivedTimeZone)) {
        selectedTimeZone = timeZoneToCheck;
      }else {
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneToCheck);
        if(timeZone != null && TimeZoneUtil.displayTimeZone(timeZone).equalsIgnoreCase(receivedTimeZone)) {
          selectedTimeZone = timeZoneToCheck;
        }
      }

      if(selectedTimeZone != null) {
        break;
      }
    }

    if(selectedTimeZone == null) {
      message.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(),
          "createClashChannelTournamentBadTimeZoneSelection")).queue();
      waitForATimeZoneSelection(member, channel, waiter, server, timeZoneIds, creationData, forceRefresh);
    }else {

      try {
        TextChannel clashChannel = channel.getGuild().createTextChannel(creationData.channelName).complete();
        List<Long> messageIds = new ArrayList<>();
        Message loadingMessage = clashChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "clashChannelLoadingMessage")).complete();
        messageIds.add(loadingMessage.getIdLong());

        ClashChannelData data = new ClashChannelData(messageIds, new ArrayList<>(), null, creationData.platform, creationData.summoner.getSummonerId(), ClashStatus.WAIT_FOR_TEAM_REGISTRATION);

        long clashChannelDbId = ClashChannelRepository.createClashChannel(server, clashChannel.getIdLong(), selectedTimeZone, data);

        ServerConfiguration config = ConfigRepository.getServerConfiguration(channel.getGuild().getIdLong(), clashChannel.getJDA());

        if(config.getZoeRoleOption().getRole() != null) {
          CommandUtil.giveRolePermission(channel.getGuild(), clashChannel, config);
        }

        channel.sendMessage(LanguageManager.getText(server.getLanguage(), "createClashChannelTournamentTimeZoneSelected")).queue();

        ClashChannel clashChannelDb = ClashChannelRepository.getClashChannelWithId(clashChannelDbId);

        TreatClashChannel clashChannelWorker = new TreatClashChannel(server, clashChannelDb, forceRefresh);

        ServerThreadsManager.getClashChannelExecutor().execute(clashChannelWorker);

      } catch(InsufficientPermissionException e) {
        channel.sendMessage(LanguageManager.getText(server.getLanguage(), "impossibleToCreateInfoChannelMissingPerms")).queue();
      } catch (SQLException e) {
        logger.error("SQLException with the ClashChannel", e);
        channel.sendMessage(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).queue();
      }
    }
  }

  private static void waitForATimeZoneSelection(Member member, TextChannel channel, EventWaiter waiter, DTO.Server server,
      List<String> timeZoneId, ClashCreationData creationData, boolean forceRefresh) {
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(member.getUser()) && e.getChannel().getId().equals(channel.getId()),
        e -> threatTimeZoneSelection(e, server, member, channel, waiter, timeZoneId, creationData, forceRefresh), 3, TimeUnit.MINUTES,
        () -> cancelCreationOfClashChannel(channel, server));
  }

  private static void cancelCreationOfClashChannel(TextChannel textChannel, Server server) {
    textChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "createClashChannelCancelMessage")).queue();
  }

}
