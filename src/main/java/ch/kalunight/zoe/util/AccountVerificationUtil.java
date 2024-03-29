package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreatePlayerCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;

public class AccountVerificationUtil {

  private static final int SIZE_OF_THE_RANDOM_STRING = 16;

  private AccountVerificationUtil() {
    //hide default public constructor
  }

  public static String getVerificationCode() {
    return "ZOE-" + RandomStringUtils.randomAlphanumeric(SIZE_OF_THE_RANDOM_STRING);
  }

  public static void verficationCodeRunnable(MessageReceivedEvent event, Server server, String verificiationCode,
      EventWaiter waiter, ZoePlatform region, SavedSummoner summoner, SavedSummoner tftSummoner, Member playerToAdd) {

    if(event.getMessage().getContentRaw().equalsIgnoreCase("DONE")) {

      String codeToCheck = null;
      try {
        codeToCheck = Zoe.getRiotApi().getThirdPartyCode(region, summoner.getSummonerId());
      } catch (APIResponseException e) {
        event.getChannel().sendMessage(RiotApiUtil.getTextHandlerRiotApiError(e, server.getLanguage())).queue();
      }

      if(verificiationCode.equals(codeToCheck)) {
        try {
          addOrCreateDBAccount(server, playerToAdd.getUser(), region, summoner, tftSummoner);
        } catch (SQLException e) {
          event.getChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).queue();
          return;
        }
        event.getChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "verificationProcessAccountValid")).queue();
      }else {
        event.getChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "verificationProcessAccountNotValid")).queue();

        waiter.waitForEvent(MessageReceivedEvent.class,
            e -> e.getAuthor().equals(playerToAdd.getUser()) && e.getChannel().equals(event.getChannel()),
            e -> verficationCodeRunnable(e, server, verificiationCode, waiter, region, summoner, tftSummoner, playerToAdd), 3, TimeUnit.MINUTES,
            () -> cancelVerification(event.getTextChannel(), server.getLanguage()));
      }
    }else {
      event.getChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "verificationProcessCancel")).queue();
    }

  }

  public static void cancelVerification(TextChannel channel, String language) {
    channel.sendMessage(LanguageManager.getText(language, "verificationProcessCancel")).queue();
  }

  public static void addOrCreateDBAccount(Server server, User member, ZoePlatform region, SavedSummoner summoner,
      SavedSummoner tftSummoner) throws SQLException {
    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, member.getIdLong());
    if(player == null) {
      PlayerRepository.createPlayer(server.serv_id, server.serv_guildId, member.getIdLong(), false);
      player = PlayerRepository.getPlayer(server.serv_guildId, member.getIdLong());
    }

    LeagueAccountRepository.createLeagueAccount(player.player_id, summoner, tftSummoner, region);
    DTO.LeagueAccount leagueAccount = LeagueAccountRepository.getLeagueAccountWithSummonerId(server.serv_guildId, summoner.getSummonerId(), region);

    CreatePlayerCommandRunnable.updateLastRank(leagueAccount);
  }
}
