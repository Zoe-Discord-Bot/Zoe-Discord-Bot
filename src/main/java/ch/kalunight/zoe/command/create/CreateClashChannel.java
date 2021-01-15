package ch.kalunight.zoe.command.create;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.ClashChannelData;
import ch.kalunight.zoe.model.dto.ClashStatus;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.BannedAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.BannedAccountRepository;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.PaginatorUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import ch.kalunight.zoe.util.TimeZoneUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class CreateClashChannel extends ZoeCommand {

  private EventWaiter waiter;

  public CreateClashChannel(EventWaiter event) {
    this.name = "clashChannel";
    this.arguments = "nameOfTheNewChannel";
    String[] aliases = {"clash", "cc"};
    this.aliases = aliases;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Permission[] botPermissionRequiered = {Permission.MANAGE_CHANNEL, Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION};
    this.botPermissions = botPermissionRequiered;
    this.guildOnly = true;
    this.help = "createClashChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommand.USAGE_NAME, name, arguments, help);
    this.waiter = event;
  }

  private class ClashCreationData {
    private String channelName;
    private Platform platform;
    private Summoner summoner;
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {

    DTO.Server server = getServer(event.getGuild().getIdLong());

    String nameChannel = event.getArgs();

    if(nameChannel == null || nameChannel.equals("")) {
      event.reply(LanguageManager.getText(server.getLanguage(), "nameOfInfochannelNeeded"));
      return;
    }

    if(nameChannel.length() > 100) {
      event.reply(LanguageManager.getText(server.getLanguage(), "nameOfTheInfoChannelNeedToBeLess100Characters"));
      return;
    }

    List<DTO.ClashChannel> dbClashChannels = ClashChannelRepository.getClashChannels(server.serv_guildId);

    for(DTO.ClashChannel dbClashChannel : dbClashChannels) {
      if(dbClashChannel != null && dbClashChannel.clashChannel_channelId != 0) {

        TextChannel clashChannel = event.getGuild().getTextChannelById(dbClashChannel.clashChannel_channelId);
        if(clashChannel == null) {
          ClashChannelRepository.deleteClashChannel(dbClashChannel.clashChannel_id);
        }
      }
    }

    ClashCreationData creationData = new ClashCreationData();

    creationData.channelName = nameChannel;

    event.reply(LanguageManager.getText(server.getLanguage(), "createClashChannelAskLeagueAccount"));

    waitForALeagueAccount(event, server, creationData);
  }

  private void waitForALeagueAccount(CommandEvent event, DTO.Server server, ClashCreationData creationData) {
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
        && !e.getMessage().getId().equals(event.getMessage().getId()),
        e -> threatLeagueAccountSelection(e, server, event, creationData), 3, TimeUnit.MINUTES,
        () -> cancelCreationOfClashChannel(event.getTextChannel(), server));
  }

  private void threatLeagueAccountSelection(MessageReceivedEvent message, Server server, CommandEvent originalEvent, ClashCreationData creationData) {

    if(message.getMessage().getContentRaw().equalsIgnoreCase("Stop")) {
      cancelCreationOfClashChannel(message.getTextChannel(), server);
      return;
    }

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(message.getMessage().getContentRaw());
    if(listArgs.size() != 2) {
      originalEvent.reply(LanguageManager.getText(server.getLanguage(), "createClashMalformedLeagueAccount"));
      waitForALeagueAccount(originalEvent, server, creationData);
      return;
    }

    String regionName = listArgs.get(0);
    String summonerName = listArgs.get(1);


    Platform region = CreatePlayerCommand.getPlatform(regionName);
    if(region == null) {
      originalEvent.reply(LanguageManager.getText(server.getLanguage(), "createClashChannelRegionTagInvalid"));
      waitForALeagueAccount(originalEvent, server, creationData);
      return;
    }

    try {
      Summoner summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);

      if(summoner != null) {

        List<DTO.ClashChannel> clashChannels = ClashChannelRepository.getClashChannels(server.serv_guildId);

        for(DTO.ClashChannel clashChannel : clashChannels) {
          if(clashChannel.clashChannel_data.getSelectedSummonerId().equals(summoner.getId())
              && clashChannel.clashChannel_data.getSelectedPlatform().equals(region)) {
            originalEvent.reply(LanguageManager.getText(server.getLanguage(), "createClashChannelAlreadyCreatedForThisSummoner"));
            waitForALeagueAccount(originalEvent, server, creationData);
            return;
          }
        }

        BannedAccount bannedAccount = BannedAccountRepository.getBannedAccount(summoner.getId(), region);
        if(bannedAccount == null) {

          originalEvent.reply(String.format(LanguageManager.getText(server.getLanguage(), "createClashChannelLeagueAccountSelected"), region.getName().toUpperCase(), summoner.getName()));

          List<String> timeZoneIds = getTimeZoneIds();

          sendListTimeZone(originalEvent, server, timeZoneIds);

          creationData.summoner = summoner;
          creationData.platform = region;

          waitForATimeZoneSelection(originalEvent, server, timeZoneIds, creationData);
        }else {
          originalEvent.reply(LanguageManager.getText(server.getLanguage(), "accountCantBeAddedOwnerChoice"));
        }
      }else {
        originalEvent.reply(String.format(LanguageManager.getText(server.getLanguage(), "createClashChannelSummonerNotFound"), summonerName, region.toString()));
      }
    } catch (SQLException e) {
      logger.error("SQLException with league account selection in create Clash Command.", e);
      originalEvent.reply(LanguageManager.getText(server.getLanguage(), "deleteLeaderboardErrorDatabase"));
    } catch (RiotApiException e) {
      RiotApiUtil.handleRiotApi(message, e, server.getLanguage());

      if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND || e.getErrorCode() == RiotApiException.SERVER_ERROR) {
        waitForALeagueAccount(originalEvent, server, creationData);
      }
    }
  }

  private void sendListTimeZone(CommandEvent originalEvent, Server server, List<String> timeZoneIds) {
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
        .setUsers(originalEvent.getAuthor())
        .setColor(Color.GREEN)
        .setText(PaginatorUtil.getPaginationTranslatedPage(server.getLanguage()))
        .setText(LanguageManager.getText(server.getLanguage(), "paginationTimeZoneList"))
        .setTimeout(3, TimeUnit.MINUTES);

    for(String timeZoneId : timeZoneIds) {
      pbuilder.addItems(TimeZoneUtil.displayTimeZone(TimeZone.getTimeZone(timeZoneId)));
    }
    
    pbuilder.build().display(originalEvent.getChannel());

  }

  private List<String> getTimeZoneIds() {
    return Arrays.asList(TimeZone.getAvailableIDs());
  }

  private void threatTimeZoneSelection(MessageReceivedEvent message, Server server, CommandEvent originalEvent, List<String> timeZoneIds, ClashCreationData creationData) {
    if(message.getMessage().getContentRaw().equalsIgnoreCase("Stop")) {
      cancelCreationOfClashChannel(message.getTextChannel(), server);
      return;
    }

    try {
      int timeZoneNumber = Integer.parseInt(message.getMessage().getContentRaw());
      if(timeZoneNumber >= 1 && timeZoneNumber < timeZoneIds.size()) {
        String timeZoneSelected = timeZoneIds.get(timeZoneNumber - 1);

        selectTimeZoneWithName(message, server, timeZoneIds, timeZoneSelected, originalEvent, creationData);
      }else {
        message.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(),
            "createClashChannelTournamentBadTimeZoneSelection")).queue();
        waitForATimeZoneSelection(originalEvent, server, timeZoneIds, creationData);
      }
    }catch(NumberFormatException e) {
      selectTimeZoneWithName(message, server, timeZoneIds, message.getMessage().getContentRaw(), originalEvent, creationData);
    }
  }

  private void selectTimeZoneWithName(MessageReceivedEvent message, Server server, List<String> timeZoneIds, String receivedTimeZone,
      CommandEvent originalEvent, ClashCreationData creationData) {
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
      waitForATimeZoneSelection(originalEvent, server, timeZoneIds, creationData);
    }else {

      try {
        TextChannel clashChannel = originalEvent.getGuild().createTextChannel(creationData.channelName).complete();
        List<Long> messageIds = new ArrayList<>();
        Message loadingMessage = clashChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "clashChannelLoadingMessage")).complete();
        messageIds.add(loadingMessage.getIdLong());

        ClashChannelData data = new ClashChannelData(messageIds, new ArrayList<>(), null, creationData.platform, creationData.summoner.getId(), ClashStatus.WAIT_FOR_GAME_START);

        ClashChannelRepository.createClashChannel(server, clashChannel.getIdLong(), selectedTimeZone, data);

        ServerConfiguration config = ConfigRepository.getServerConfiguration(originalEvent.getGuild().getIdLong());

        if(config.getZoeRoleOption().getRole() != null) {
          CommandUtil.giveRolePermission(originalEvent.getGuild(), clashChannel, config);
        }

        originalEvent.reply(LanguageManager.getText(server.getLanguage(), "createClashChannelTournamentTimeZoneSelected"));

      } catch(InsufficientPermissionException e) {
        originalEvent.reply(LanguageManager.getText(server.getLanguage(), "impossibleToCreateInfoChannelMissingPerms"));
      } catch (SQLException e) {
        logger.error("SQLException with the ClashChannel", e);
        originalEvent.reply(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport"));
      }
    }
  }

  private void waitForATimeZoneSelection(CommandEvent event, DTO.Server server, List<String> timeZoneId, ClashCreationData creationData) {
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
        && !e.getMessage().getId().equals(event.getMessage().getId()),
        e -> threatTimeZoneSelection(e, server, event, timeZoneId, creationData), 3, TimeUnit.MINUTES,
        () -> cancelCreationOfClashChannel(event.getTextChannel(), server));
  }

  private void cancelCreationOfClashChannel(TextChannel textChannel, Server server) {
    textChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "createClashChannelCancelMessage")).queue();
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
