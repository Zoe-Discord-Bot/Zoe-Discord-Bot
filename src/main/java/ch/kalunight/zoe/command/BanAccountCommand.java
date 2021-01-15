package ch.kalunight.zoe.command;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
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
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class BanAccountCommand extends ZoeCommand {

  private static final Logger logger = LoggerFactory.getLogger(BanAccountCommand.class);

  private static final int SIZE_OF_THE_RANDOM_STRING = 16;

  private EventWaiter waiter;

  public BanAccountCommand(EventWaiter eventWaiter) {
    this.name = "banAccount";
    this.help = "banAccountCommandHelp";
    String[] aliasesTable = {"ban", "banList", "banAccountList"};
    this.aliases = aliasesTable;
    this.waiter = eventWaiter;
    this.hidden = false;
    this.ownerCommand = false;
    this.guildOnly = false;
    this.cooldown = 180;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {

    final String language;

    if(event.getChannelType().equals(ChannelType.TEXT)) {
      Server server = getServer(event.getGuild().getIdLong());
      if(server != null) {
        language = server.getLanguage();
      }else {
        language = LanguageManager.DEFAULT_LANGUAGE;
      }
    }else {
      language = LanguageManager.DEFAULT_LANGUAGE;
    }

    String accountVerificationCode = "ZOE-" + RandomStringUtils.randomAlphanumeric(SIZE_OF_THE_RANDOM_STRING);

    event.reply(String.format(LanguageManager.getText(language, "banAccountCommandProveAccountOwner"), accountVerificationCode));

    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
        && !e.getMessage().getId().equals(event.getMessage().getId()),
        e -> accountReceived(e, language, accountVerificationCode), 3, TimeUnit.MINUTES,
        () -> cancelVerification(event.getEvent(), language));
  }

  private void accountReceived(MessageReceivedEvent event, String language, String codeExpected) {
    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getMessage().getContentRaw());

    if(event.getMessage().getContentRaw().equalsIgnoreCase("STOP")) {
      event.getChannel().sendMessage(LanguageManager.getText(language, "banAccountCommandStopManageProcess")).queue();
      return;
    }

    if(listArgs.size() != 2) {
      event.getChannel().sendMessage(LanguageManager.getText(language, "banAccountCommandMalformedAccount")).queue();

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> accountReceived(e, language, codeExpected), 3, TimeUnit.MINUTES,
          () -> cancelVerification(event, language));
      return;
    }

    Platform region = CreatePlayerCommand.getPlatform(listArgs.get(0));
    if(region == null) {
      event.getChannel().sendMessage(LanguageManager.getText(language, "banAccountCommandInvalidRegionTag")).queue();

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> accountReceived(e, language, codeExpected), 3, TimeUnit.MINUTES,
          () -> cancelVerification(event, language));

      return;
    }

    String summonerName = listArgs.get(1);

    Summoner summoner;
    String code = "";

    try {
      summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
    } catch (RiotApiException error) {
      RiotApiUtil.handleRiotApi(event, error, language);

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> accountReceived(e, language, codeExpected), 3, TimeUnit.MINUTES,
          () -> cancelVerification(event, language));

      return;
    }

    try {
      code = Zoe.getRiotApi().getValidationCode(region, summoner.getId());
    }catch (RiotApiException error) {
      if(error.getErrorCode() == 404) {
        event.getChannel().sendMessage(String.format(LanguageManager.getText(language, "banAccountCommandInvalidVerificationTag"), codeExpected, event.getMessage().getContentRaw())).queue();
      }else {
        RiotApiUtil.handleRiotApi(event, error, language);
      }

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> accountReceived(e, language, codeExpected), 3, TimeUnit.MINUTES,
          () -> cancelVerification(event, language));

      return;
    }

    if(codeExpected.equals(code)) {
      //Owner OK !
      startAccountManagementPanel(summoner, region, event, language);
    }else {
      event.getChannel().sendMessage(String.format(LanguageManager.getText(language, "banAccountCommandInvalidVerificationTag"), codeExpected, event.getMessage().getContentRaw())).queue();

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> accountReceived(e, language, codeExpected), 3, TimeUnit.MINUTES,
          () -> cancelVerification(event, language));
    }

  }

  private void cancelVerification(MessageReceivedEvent event, String lang) {
    event.getChannel().sendMessage(LanguageManager.getText(lang, "banAccountCommandCancelVerification")).queue();
  }

  private void startAccountManagementPanel(Summoner summoner, Platform region, MessageReceivedEvent event,
      String language) {

    List<Server> serversWithTheAccount = null;
    BannedAccount accountInTheBanList;

    try {
      serversWithTheAccount = ServerRepository.getServersWithLeagueAccountIdAndRegion(summoner.getId(), region);
      accountInTheBanList = BannedAccountRepository.getBannedAccount(summoner.getId(), region);
    } catch (SQLException e) {
      event.getChannel().sendMessage(LanguageManager.getText(language, "errorSQLPleaseReport")).queue();
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
        Guild guild = Zoe.getJda().getGuildById(server.serv_guildId);
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
      listOfGuilds.display(event.getChannel());
    }else {
      event.getChannel().sendMessage(LanguageManager.getText(language, "banAccountCommandAccountIn0Server")).queue();
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
        e -> messageInterpretor(e, language, summoner, region, serversInOrder), 3, TimeUnit.MINUTES,
        () -> stopProcess(event, language));
  }

  private void messageInterpretor(MessageReceivedEvent event, String language, Summoner concernedSummoner, Platform region,
      List<Server> serverList) {
    String messageReceived = event.getMessage().getContentRaw();
    MessageChannel responseChannel = event.getChannel();
    responseChannel.sendTyping().complete();

    String[] messagePart = messageReceived.split("\\s+");

    if(messagePart.length == 1) {
      if(messagePart[0].equalsIgnoreCase("kick")) {
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandKickMissingOrBadSelection")).queue();
        waitForAnotherResponse(event, language, concernedSummoner, region, serverList);
      }else if(messagePart[0].equalsIgnoreCase("ban")) {
        handleBanCommand(event, language, concernedSummoner, region, responseChannel, serverList);
      }else if(messagePart[0].equalsIgnoreCase("stop")) {
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandStopManageProcess")).queue();
      }else {
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandInvalidCommand")).queue();
        waitForAnotherResponse(event, language, concernedSummoner, region, serverList);
      }
    }else if(messagePart.length >= 2) {
      if(messagePart[0].equalsIgnoreCase("kick")) {
        handleKickCommand(language, concernedSummoner, region, serverList, responseChannel, messagePart, event);
      }else if(messagePart[0].equalsIgnoreCase("ban")) {
        handleBanCommand(event, language, concernedSummoner, region, responseChannel, serverList);
      }else if(messagePart[0].equalsIgnoreCase("stop")) {
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandStopManageProcess")).queue();
      }else {
        responseChannel.sendMessage(LanguageManager.getText(language, "banAccountCommandInvalidCommand")).queue();
        waitForAnotherResponse(event, language, concernedSummoner, region, serverList);
      }
    }
  }

  private void handleKickCommand(String language, Summoner concernedSummoner, Platform region, List<Server> serverList,
      MessageChannel responseChannel, String[] messagePart, MessageReceivedEvent event) {
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
    waitForAnotherResponse(event, language, concernedSummoner, region, serverList);
  }

  private void handleBanCommand(MessageReceivedEvent event, String language, Summoner concernedSummoner, Platform region,
      MessageChannel responseChannel, List<Server> serverList) {
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
    waitForAnotherResponse(event, language, concernedSummoner, region, serverList);
  }

  private void waitForAnotherResponse(MessageReceivedEvent event, String language, Summoner summoner, Platform region,
      List<Server> serverList) {
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
        && !e.getMessage().getId().equals(event.getMessage().getId()),
        e -> messageInterpretor(e, language, summoner, region, serverList), 3, TimeUnit.MINUTES,
        () -> stopProcess(event, language));
  }

  private void stopProcess(MessageReceivedEvent event, String language) {
    event.getChannel().sendMessage(LanguageManager.getText(language, "banAccountCommandCancelVerification")).queue();
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
