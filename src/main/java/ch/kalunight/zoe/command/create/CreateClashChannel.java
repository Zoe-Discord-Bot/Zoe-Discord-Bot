package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.service.infochannel.InfoPanelRefresher;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
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
    this.guildOnly = true;
    this.help = "createClashChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommand.USAGE_NAME, name, arguments, help);
    this.waiter = event;
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {

    DTO.Server server = getServer(event.getGuild().getIdLong());

    String nameChannel = event.getArgs();

    if(nameChannel == null || nameChannel.equals("")) {
      event.reply(LanguageManager.getText(server.serv_language, "nameOfInfochannelNeeded"));
      return;
    }

    if(nameChannel.length() > 100) {
      event.reply(LanguageManager.getText(server.serv_language, "nameOfTheInfoChannelNeedToBeLess100Characters"));
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
    
    event.reply(LanguageManager.getText(server.serv_language, "createClashChannelAskLeagueAccount"));
    
    waitForALeagueAccount(event, server);

    /**

    try {
      TextChannel infoChannel = event.getGuild().createTextChannel(nameChannel).complete();
      InfoChannelRepository.createInfoChannel(server.serv_id, infoChannel.getIdLong());
      Message message = infoChannel.sendMessage(LanguageManager.getText(server.serv_language, "infoChannelTitle")
          + "\n \n" + LanguageManager.getText(server.serv_language, "loading")).complete();

      dbInfochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);
      InfoChannelRepository.createInfoPanelMessage(dbInfochannel.infoChannel_id, message.getIdLong());

      ServerConfiguration config = ConfigRepository.getServerConfiguration(event.getGuild().getIdLong());

      if(config.getZoeRoleOption().getRole() != null) {
        CommandUtil.giveRolePermission(event.getGuild(), infoChannel, config);
      }

      event.reply(LanguageManager.getText(server.serv_language, "channelCreatedMessage"));

      if(!ServerThreadsManager.isServerWillBeTreated(server)) {
        ServerThreadsManager.getServersIsInTreatment().put(event.getGuild().getId(), true);
        ServerRepository.updateTimeStamp(server.serv_guildId, LocalDateTime.now());
        ServerThreadsManager.getServerExecutor().execute(new InfoPanelRefresher(server, false));
      }
    } catch(InsufficientPermissionException e) {
      event.reply(LanguageManager.getText(server.serv_language, "impossibleToCreateInfoChannelMissingPerms"));
    }

     */
  }

  private void waitForALeagueAccount(CommandEvent event, DTO.Server server) {
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
        && !e.getMessage().getId().equals(event.getMessage().getId()),
        e -> threatLeagueAccountSelection(e, server, event), 3, TimeUnit.MINUTES,
        () -> cancelCreationOfClashChannel(event.getTextChannel(), server));
  }

  private void threatLeagueAccountSelection(MessageReceivedEvent message, Server server, CommandEvent originalEvent) {
    
    if(message.getMessage().getContentRaw().equalsIgnoreCase("Stop")) {
      cancelCreationOfClashChannel(message.getTextChannel(), server);
      return;
    }
    
    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(message.getMessage().getContentRaw());
    if(listArgs.size() != 2) {
      originalEvent.reply(LanguageManager.getText(server.serv_language, "createClashMalformedLeagueAccount"));
      waitForALeagueAccount(originalEvent, server);
      return;
    }
    
    String regionName = listArgs.get(0);
    String summonerName = listArgs.get(1);


    Platform region = CreatePlayerCommand.getPlatform(regionName);
    if(region == null) {
      originalEvent.reply(LanguageManager.getText(server.serv_language, "createClashChannelRegionTagInvalid"));
      waitForALeagueAccount(originalEvent, server);
      return;
    }
    
    try {
      Summoner summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
      
      if(summoner != null) {
        
        List<DTO.ClashChannel> clashChannels = ClashChannelRepository.getClashChannels(server.serv_guildId);
        
        for(DTO.ClashChannel clashChannel : clashChannels) {
          if(clashChannel.clashChannel_data.getSelectedSummonerId().equals(summoner.getId())
              && clashChannel.clashChannel_data.getSelectedPlatform().equals(region)) {
            originalEvent.reply(LanguageManager.getText(server.serv_language, "createClashChannelAlreadyCreatedForThisSummoner"));
            waitForALeagueAccount(originalEvent, server);
            return;
          }
        }
        
        originalEvent.reply(String.format(LanguageManager.getText(server.serv_language, "createClashChannelLeagueAccountSelected"), region.toString(), summoner.getName()));
        //TODO: Wait for timezone
      }else {
        originalEvent.reply(String.format(LanguageManager.getText(server.serv_language, "createClashChannelSummonerNotFound"), summonerName, region.toString()));
      }
    } catch (SQLException e) {
      logger.error("SQLException with league account selection in create Clash Command.", e);
      originalEvent.reply(LanguageManager.getText(server.serv_language, "deleteLeaderboardErrorDatabase"));
    } catch (RiotApiException e) {
      RiotApiUtil.handleRiotApi(message, e, server.serv_language);
      
      if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND || e.getErrorCode() == RiotApiException.SERVER_ERROR) {
        waitForALeagueAccount(originalEvent, server);
      }
    }
  }

  private void cancelCreationOfClashChannel(TextChannel textChannel, Server server) {
    textChannel.sendMessage(LanguageManager.getText(server.serv_language, "createClashChannelCancelMessage")).queue();
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
