package ch.kalunight.zoe.command;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class BanAccountCommand extends ZoeCommand {

  private Random random = new Random();

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
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {

    final String language;

    if(event.getChannelType().equals(ChannelType.TEXT)) {
      Server server = getServer(event.getGuild().getIdLong());
      if(server != null) {
        language = server.serv_language;
      }else {
        language = LanguageManager.DEFAULT_LANGUAGE;
      }
    }else {
      language = LanguageManager.DEFAULT_LANGUAGE;
    }

    String accountVerificationCode = "ZOE-" + (100000 + random.nextInt(900000));

    event.reply(String.format(LanguageManager.getText(language, "banAccountCommandProveAccountOwner"), accountVerificationCode));

    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
        && !e.getMessage().getId().equals(event.getMessage().getId()),
        e -> accountReceived(e, language, accountVerificationCode), 3, TimeUnit.MINUTES,
        () -> cancelVerification(event.getEvent(), language));
  }

  private void accountReceived(MessageReceivedEvent event, String language, String codeExpected) {
    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getMessage().getContentRaw());

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

  private void startAccountManagementPanel(Summoner summoner, Platform region, MessageReceivedEvent event,
      String language) {
    
  }

  private void cancelVerification(MessageReceivedEvent event, String lang) {
    event.getChannel().sendMessage(LanguageManager.getText(lang, "banAccountCommandCancelVerification")).queue();
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
