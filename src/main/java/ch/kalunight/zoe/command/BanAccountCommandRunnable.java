package ch.kalunight.zoe.command;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreatePlayerCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.BannedAccount;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.BannedAccountRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.PaginatorUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class BanAccountCommandRunnable {

  private static final Logger logger = LoggerFactory.getLogger(BanAccountCommandRunnable.class);

  private static final int SIZE_OF_THE_RANDOM_STRING = 16;

  private BanAccountCommandRunnable() {
    // hide default constructor
  }
  
  public static void executeCommand(String language, EventWaiter waiter, MessageChannel channel, Guild guild, User author, Message toEdit, InteractionHook hook) {

    String accountVerificationCode = "ZOE-" + RandomStringUtils.randomAlphanumeric(SIZE_OF_THE_RANDOM_STRING);

    CommandUtil.sendMessageWithClassicOrSlashCommand(String.format(LanguageManager.getText(language, "banAccountCommandProveAccountOwner"), accountVerificationCode), toEdit, hook);

    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(author) && e.getChannel().equals(channel),
        e -> accountReceived(e, language, accountVerificationCode, waiter), 3, TimeUnit.MINUTES,
        () -> cancelVerification(channel, language));
  }

  private static void accountReceived(MessageReceivedEvent event, String language, String codeExpected, EventWaiter waiter) {
    List<String> listArgs = CreatePlayerCommandRunnable.getParameterInParenteses(event.getMessage().getContentRaw());

    if(event.getMessage().getContentRaw().equalsIgnoreCase("STOP")) {
      event.getChannel().sendMessage(LanguageManager.getText(language, "banAccountCommandStopManageProcess")).queue();
      return;
    }

    if(listArgs.size() != 2) {
      event.getChannel().sendMessage(LanguageManager.getText(language, "banAccountCommandMalformedAccount")).queue();

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> accountReceived(e, language, codeExpected, waiter), 3, TimeUnit.MINUTES,
          () -> cancelVerification(event.getChannel(), language));
      return;
    }

    Platform region = CreatePlayerCommandRunnable.getPlatform(listArgs.get(0));
    if(region == null) {
      event.getChannel().sendMessage(LanguageManager.getText(language, "banAccountCommandInvalidRegionTag")).queue();

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> accountReceived(e, language, codeExpected, waiter), 3, TimeUnit.MINUTES,
          () -> cancelVerification(event.getChannel(), language));

      return;
    }

    String summonerName = listArgs.get(1);

    Message message = event.getTextChannel().sendMessage(LanguageManager.getText(language, "loadingSummoner")).complete();
    
    Summoner summoner;
    String code = "";

    try {
      summoner = Zoe.getRiotApi().getSummonerByNameWithRateLimit(region, summonerName);
    } catch (RiotApiException error) {
      RiotApiUtil.handleRiotApi(message, error, language);

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> accountReceived(e, language, codeExpected, waiter), 3, TimeUnit.MINUTES,
          () -> cancelVerification(event.getChannel(), language));

      return;
    }

    try {
      code = Zoe.getRiotApi().getValidationCode(region, summoner.getId());
    }catch (RiotApiException error) {
      if(error.getErrorCode() == 404) {
        message.editMessage(String.format(LanguageManager.getText(language, "banAccountCommandInvalidVerificationTag"), codeExpected, event.getMessage().getContentRaw())).queue();
      }else {
        RiotApiUtil.handleRiotApi(message, error, language);
      }

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> accountReceived(e, language, codeExpected, waiter), 3, TimeUnit.MINUTES,
          () -> cancelVerification(event.getChannel(), language));

      return;
    }

    if(codeExpected.equals(code)) {
      //Owner OK !
      startAccountManagementPanel(summoner, region, event, language, message, waiter);
    }else {
      message.editMessage(String.format(LanguageManager.getText(language, "banAccountCommandInvalidVerificationTag"), codeExpected, event.getMessage().getContentRaw())).queue();

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> accountReceived(e, language, codeExpected, waiter), 3, TimeUnit.MINUTES,
          () -> cancelVerification(event.getChannel(), language));
    }

  }

  private static void cancelVerification(MessageChannel channel, String lang) {
    channel.sendMessage(LanguageManager.getText(lang, "banAccountCommandCancelVerification")).queue();
  }

  private static void startAccountManagementPanel(Summoner summoner, Platform region, MessageReceivedEvent event,
      String language, Message messageToEdit, EventWaiter waiter) {

    List<Server> serversWithTheAccount = null;
    BannedAccount accountInTheBanList;

    try {
      serversWithTheAccount = ServerRepository.getServersWithLeagueAccountIdAndRegion(summoner.getId(), region);
      accountInTheBanList = BannedAccountRepository.getBannedAccount(summoner.getId(), region);
    } catch (SQLException e) {
      messageToEdit.editMessage(LanguageManager.getText(language, "errorSQLPleaseReport")).queue();
      logger.error("SQL error while starting ban account panel", e);
      return;
    }

    List<Server> serversInOrder = new ArrayList<>();
    if(!serversWithTheAccount.isEmpty()) {
      Paginator.Builder pbuilder = new Paginator.Builder()
          .setColumns(1)
          .setItemsPerPage(10)
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
          .setUsers(event.getAuthor())
          .setColor(Color.GREEN)
          .setText(PaginatorUtil.getPaginationTranslatedPage(language))
          .setText(LanguageManager.getText(language, "banAccountCommandListOfGuild"))
          .setTimeout(3, TimeUnit.MINUTES);
      
      for(Server server : serversWithTheAccount) {
        serversInOrder.add(server);
        Guild guild = event.getJDA().getGuildById(server.serv_guildId);
        if(guild != null) {
          try {
            guild.retrieveMember(event.getAuthor()).complete();
            pbuilder.addItems(String.format(LanguageManager.getText(language, "banAccountCommandGuildItemWithName"), guild.getName(), guild.getIdLong()));
          }catch(ErrorResponseException e) {
            pbuilder.addItems(String.format(LanguageManager.getText(language, "banAccountCommandGuildItemOnlyId"), guild.getIdLong()));
          }
        }else {
          pbuilder.addItems(String.format(LanguageManager.getText(language, "banAccountCommandGuildItemOnlyId"), server.serv_guildId));
        }
      }

      
      
      Paginator listOfGuilds = pbuilder.build();
      listOfGuilds.display(messageToEdit);
    }else {
      messageToEdit.editMessage(LanguageManager.getText(language, "banAccountCommandAccountIn0Server")).queue();
    }

    String statusOfLeagueAccount;
    if(accountInTheBanList == null) {
      statusOfLeagueAccount = LanguageManager.getText(language, "banAccountCommandStatusPossibleToAdd");
    }else {
      statusOfLeagueAccount = LanguageManager.getText(language, "banAccountCommandStatusImpossibleToAdd");
    }

    event.getChannel().sendMessage(String.format(LanguageManager.getText(language, "banAccountCommandControlMessage"),
        statusOfLeagueAccount)).queue();

    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
        && !e.getMessage().getId().equals(event.getMessage().getId()),
        e -> messageInterpretor(e, language, summoner, region, serversInOrder, waiter), 3, TimeUnit.MINUTES,
        () -> stopProcess(event, language));
  }

  private static void messageInterpretor(MessageReceivedEvent event, String language, Summoner concernedSummoner, Platform region,
      List<Server> serverList, EventWaiter waiter) {
    String messageReceived = event.getMessage().getContentRaw();
    MessageChannel responseChannel = event.getChannel();
    responseChannel.sendTyping().complete();

    String[] messagePart = messageReceived.split("\\s+");

    if(messagePart.length == 1) {
      if(messagePart[0].equalsIgnoreCase("kick")) {
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandKickMissingOrBadSelection")).queue();
        waitForAnotherResponse(event, language, concernedSummoner, region, serverList, waiter);
      }else if(messagePart[0].equalsIgnoreCase("ban")) {
        handleBanCommand(event, language, concernedSummoner, region, responseChannel, serverList, waiter);
      }else if(messagePart[0].equalsIgnoreCase("stop")) {
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandStopManageProcess")).queue();
      }else {
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandInvalidCommand")).queue();
        waitForAnotherResponse(event, language, concernedSummoner, region, serverList, waiter);
      }
    }else if(messagePart.length >= 2) {
      if(messagePart[0].equalsIgnoreCase("kick")) {
        handleKickCommand(language, concernedSummoner, region, serverList, responseChannel, messagePart, event, waiter);
      }else if(messagePart[0].equalsIgnoreCase("ban")) {
        handleBanCommand(event, language, concernedSummoner, region, responseChannel, serverList, waiter);
      }else if(messagePart[0].equalsIgnoreCase("stop")) {
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandStopManageProcess")).queue();
      }else {
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandInvalidCommand")).queue();
        waitForAnotherResponse(event, language, concernedSummoner, region, serverList, waiter);
      }
    }
  }

  private static void handleKickCommand(String language, Summoner concernedSummoner, Platform region, List<Server> serverList,
      MessageChannel responseChannel, String[] messagePart, MessageReceivedEvent event, EventWaiter waiter) {
    String selectedServer = messagePart[1];
    Integer selectedServerInt;

    try {
      selectedServerInt = Integer.parseInt(selectedServer);
    }catch(NumberFormatException e) {
      selectedServerInt = null;
    }

    if(selectedServerInt == null && selectedServer.equalsIgnoreCase("all")) {

      try {
        List<LeagueAccount> leagueAccountOfTheSummoner = 
            LeagueAccountRepository.getLeaguesAccountsWithSummonerIdAndServer(concernedSummoner.getId(), region);

        if(!leagueAccountOfTheSummoner.isEmpty()) {
          
          for(LeagueAccount leagueAccount : leagueAccountOfTheSummoner) {
            LeagueAccountRepository.deleteAccountWithId(leagueAccount.leagueAccount_id);
          }

          responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandDeleteAccountFromAllServer")).queue();
        }else {
          responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandKickAllNoServer")).queue();
        }
      } catch(SQLException e) {
        responseChannel.sendMessage(LanguageManager.getText(language, "errorSQLPleaseReport")).queue();
        logger.error("SQL Error while deleting all occurence of the account", e);
      }

    }else if(selectedServerInt != null && selectedServerInt <= serverList.size() && selectedServerInt >= 1) {
      Server server = serverList.get(selectedServerInt - 1);

      try {
        LeagueAccount leagueAccount = LeagueAccountRepository.getLeagueAccountWithSummonerId(server.serv_guildId, concernedSummoner.getId(), region);
        if(leagueAccount == null) {
          responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandAccountAlreadyDeletedInThisServer")).queue();
        }else {
          LeagueAccountRepository.deleteAccountWithId(leagueAccount.leagueAccount_id);
          responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandSelectedAccountCorrectlyDeleted")).queue();
        }

      } catch(SQLException e) {
        responseChannel.sendMessage(LanguageManager.getText(language, "errorSQLPleaseReport")).queue();
        logger.error("SQL Error while deleting a specific account", e);
      }
    }else {
      responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandBadKickCommand")).queue();
    }
    waitForAnotherResponse(event, language, concernedSummoner, region, serverList, waiter);
  }

  private static void handleBanCommand(MessageReceivedEvent event, String language, Summoner concernedSummoner, Platform region,
      MessageChannel responseChannel, List<Server> serverList, EventWaiter waiter) {
    try {
      BannedAccount bannedAccount = BannedAccountRepository.getBannedAccount(concernedSummoner.getId(), region);
      if(bannedAccount == null) {
        BannedAccountRepository.createBannedAccount(concernedSummoner.getId(), region);
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandAccountAddedToTheBanList")).queue();
      }else {
        BannedAccountRepository.deleteBannedAccount(bannedAccount.banAcc_id);
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandAccountDeleteOfTheBanList")).queue();
      }
    } catch(SQLException e) {
      responseChannel.sendMessage(LanguageManager.getText(language, "errorSQLPleaseReport")).queue();
      logger.error("SQL Error while adding a Banned Account", e);
    }
    waitForAnotherResponse(event, language, concernedSummoner, region, serverList, waiter);
  }

  private static void waitForAnotherResponse(MessageReceivedEvent event, String language, Summoner summoner, Platform region,
      List<Server> serverList, EventWaiter waiter) {
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
        && !e.getMessage().getId().equals(event.getMessage().getId()),
        e -> messageInterpretor(e, language, summoner, region, serverList, waiter), 3, TimeUnit.MINUTES,
        () -> stopProcess(event, language));
  }

  private static void stopProcess(MessageReceivedEvent event, String language) {
    event.getChannel().sendMessage(LanguageManager.getText(language, "banAccountCommandCancelVerification")).queue();
  }
}
