package ch.kalunight.zoe.command.add;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreatePlayerCommandRunnable;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.BannedAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.repositories.BannedAccountRepository;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.AccountVerificationUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.impl.lol.builders.summoner.SummonerBuilder;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class AddAccountCommandRunnable {

  public static final String USAGE_NAME = "account";
  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");

  private AddAccountCommandRunnable() {
    // hide default public constructor
  }

  public static String executeCommand(Server server, Member author, List<Member> mentionnedMembers, String args, EventWaiter waiter, TextChannel channel) throws SQLException {

    ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId, author.getJDA());

    if(!config.getUserSelfAdding().isOptionActivated() && !author.getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      return String.format(LanguageManager.getText(server.getLanguage(), "permissionNeededMessage"),
          Permission.MANAGE_CHANNEL.getName());
    }

    User user = CreatePlayerCommandRunnable.getMentionedUser(mentionnedMembers);
    if(user == null) {
      return String.format(LanguageManager.getText(server.getLanguage(), "mentionNeededMessageWithUser"),
          author.getUser().getName());
    }else if(!user.getId().equals(author.getUser().getId()) && !author.getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      return LanguageManager.getText(server.getLanguage(), "permissionNeededUpdateOtherPlayer");
    }

    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
    if(player == null) {
      return LanguageManager.getText(server.getLanguage(), "mentionnedUserNotRegistered");
    }

    RegionOption regionOption = config.getDefaultRegion();

    List<String> listArgs = CreatePlayerCommandRunnable.getParameterInParenteses(args);
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      return String.format(LanguageManager.getText(server.getLanguage(), "addCommandMalformedWithoutRegionOption"), USAGE_NAME);
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      return String.format(LanguageManager.getText(server.getLanguage(), "addCommandMalformedWithRegionOption"),
          USAGE_NAME, regionOption.getRegion().getRealmValue().toUpperCase());
    }

    String regionName;
    String summonerName;
    if(listArgs.size() == 2) {
      regionName = listArgs.get(0);
      summonerName = listArgs.get(1);
    }else {
      regionName = regionOption.getRegion().getRealmValue();
      summonerName = listArgs.get(0);
    }

    LeagueShard region = CreatePlayerCommandRunnable.getPlatform(regionName);
    if(region == null) {
      return LanguageManager.getText(server.getLanguage(), "regionTagInvalid");
    }

    Summoner summoner;
    Summoner tftSummoner;
    try {
      summoner = new SummonerBuilder().withPlatform(region).withName(summonerName).get();
      tftSummoner = Zoe.getTftSummonerApi().getSummonerByAccount(region, summonerName);
    }catch(APIResponseException e) {
      return RiotApiUtil.getTextHandlerRiotApiError(e, server.getLanguage());
    }

    LeagueAccount newAccount = new LeagueAccount(summoner, region);

    DTO.Player playerAlreadyWithTheAccount = PlayerRepository
        .getPlayerByLeagueAccountAndGuild(server.serv_guildId, summoner.getSummonerId(), region);

    if(playerAlreadyWithTheAccount != null) {
      User userAlreadyWithTheAccount = author.getJDA().retrieveUserById(playerAlreadyWithTheAccount.player_discordId).complete();
      return String.format(LanguageManager.getText(server.getLanguage(), "accountAlreadyLinkedToAnotherPlayer"), 
          userAlreadyWithTheAccount.getName());
    }

    BannedAccount bannedAccount = BannedAccountRepository.getBannedAccount(summoner.getSummonerId(), region);
    if(bannedAccount == null) {

      if(config.getForceVerificationOption().isOptionActivated() && !author.getPermissions().contains(Permission.MANAGE_CHANNEL)) {

        String verificiationCode = AccountVerificationUtil.getVerificationCode();

        String message = String.format(LanguageManager.getText(server.getLanguage(), "verificationProcessWhileAddingAccount"),
            region.getRealmValue().toUpperCase(), summoner.getName(), verificiationCode);

        waiter.waitForEvent(MessageReceivedEvent.class,
            e -> e.getAuthor().equals(author.getUser()) && e.getChannel().equals(channel),
            e -> AccountVerificationUtil.verficationCodeRunnable(e, server, verificiationCode, waiter, region, summoner, tftSummoner, author), 3, TimeUnit.MINUTES,
            () -> AccountVerificationUtil.cancelVerification(channel, server.getLanguage()));

        return message;
      }else {
        AccountVerificationUtil.addOrCreateDBAccount(server, author, region, summoner, tftSummoner);

        return String.format(LanguageManager.getText(server.getLanguage(), "accountAddedToPlayer"),
            newAccount.getSummoner().getName(), user.getName());
      }
    }else {
      return LanguageManager.getText(server.getLanguage(), "accountCantBeAddedOwnerChoice");
    }
  }

}
