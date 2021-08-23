package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.BannedAccount;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.BannedAccountRepository;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.LastRankRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.AccountVerificationUtil;
import ch.kalunight.zoe.util.LastRankUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.impl.lol.builders.league.LeagueBuilder;
import no.stelar7.api.r4j.impl.lol.builders.summoner.SummonerBuilder;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class CreatePlayerCommandRunnable {

  private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");

  private CreatePlayerCommandRunnable() {
    // hide default public constructor
  }

  public static String executeCommand(Server server, JDA jda, Member author, List<Member> mentionnedMembers,
      String args, EventWaiter waiter, TextChannel channel) throws SQLException {

    ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId, jda);

    if(!config.getUserSelfAdding().isOptionActivated() && !author.getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      return String.format(LanguageManager.getText(server.getLanguage(), "permissionNeededMessage"),
          Permission.MANAGE_CHANNEL.getName());
    }

    User user = getMentionedUser(mentionnedMembers);
    if(user == null) {
      return String.format(LanguageManager.getText(server.getLanguage(), "mentionNeededMessageWithUser"), author.getUser().getName());
    }else if(user.getIdLong() != author.getIdLong() && !author.getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      return String.format(LanguageManager.getText(server.getLanguage(), "permissionNeededCreateOtherPlayer"),
          Permission.MANAGE_CHANNEL.getName());
    }

    if(isTheGivenUserAlreadyRegister(user, server)) {
      return LanguageManager.getText(server.getLanguage(), "createPlayerAlreadyRegistered");
    }

    RegionOption regionOption = config.getDefaultRegion();

    List<String> listArgs = CreatePlayerCommandRunnable.getParameterInParenteses(args);
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      return LanguageManager.getText(server.getLanguage(), "createPlayerMalformedWithoutRegionOption");
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      return String.format(LanguageManager.getText(server.getLanguage(), "createPlayerMalformedWithRegionOption"), 
          regionOption.getRegion().getRealmValue().toUpperCase());
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


    LeagueShard region = getPlatform(regionName);
    if(region == null) {
      return LanguageManager.getText(server.getLanguage(), "regionTagInvalid");
    }

    Summoner summoner; 
    Summoner tftSummoner; 

    try {
      summoner = new SummonerBuilder().withPlatform(region).withName(summonerName).get();
      tftSummoner = Zoe.getTftSummonerApi().getSummonerByName(region, summonerName);
    }catch(APIResponseException e) {
      return RiotApiUtil.getTextHandlerRiotApiError(e, server.getLanguage());
    }

    DTO.Player playerAlreadyWithTheAccount = PlayerRepository
        .getPlayerByLeagueAccountAndGuild(server.serv_guildId, summoner.getSummonerId(), region);

    if(playerAlreadyWithTheAccount != null) {
      return String.format(LanguageManager.getText(server.getLanguage(), "accountAlreadyLinkedToAnotherPlayer"),
          playerAlreadyWithTheAccount.retrieveUser(jda).getName());
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
        PlayerRepository.createPlayer(server.serv_id, server.serv_guildId, user.getIdLong(), false);
        DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
        LeagueAccountRepository.createLeagueAccount(player.player_id, summoner, tftSummoner, region.getRealmValue());

        LeagueAccount leagueAccount = 
            LeagueAccountRepository.getLeagueAccountWithSummonerId(server.serv_guildId, summoner.getSummonerId(), region);

        updateLastRank(leagueAccount);

        if(config.getZoeRoleOption().getRole() != null) {
          Member member = author.getGuild().retrieveMember(user).complete();
          if(member != null) {
            author.getGuild().addRoleToMember(member, config.getZoeRoleOption().getRole()).queue();
          }
        }

        return String.format(LanguageManager.getText(server.getLanguage(), "createPlayerDoneMessage"),
            user.getName(), summoner.getName());
      }
    }else {
      return LanguageManager.getText(server.getLanguage(), "accountCantBeAddedOwnerChoice");
    }
  }

  public static void updateLastRank(LeagueAccount leagueAccount) throws SQLException {
    LastRankRepository.createLastRank(leagueAccount.leagueAccount_id);
    LastRank lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);

    try {
      List<LeagueEntry> leagueEntries = Zoe.getRiotApi().getLoLAPI().getLeagueAPI().getLeagueEntries(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);
      LastRankUtil.updateLoLLastRank(lastRank, leagueEntries);
    } catch(APIResponseException e) {
      Zoe.logger.info("Fail to refresh LoL last rank while creating a leagueAccount, will be done at the next game.");
    }

    try {
      List<LeagueEntry> tftLeagueEntries = Zoe.getRiotApi().getTFTAPI()
          .getLeagueAPI().getLeagueEntries(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_tftSummonerId);
      LastRankUtil.updateTFTLastRank(lastRank, tftLeagueEntries);
    } catch(APIResponseException e) {
      Zoe.logger.info("Fail to refresh TFT last rank while creating a leagueAccount, will be done at the next game.");
    }
  }

  public static LeagueShard getPlatform(String regionName) {
    Optional<LeagueShard> region = LeagueShard.fromString(regionName);

    if(region.isPresent()) {
      return region.get();
    }else {
      return null;
    }
  }

  public static List<String> getParameterInParenteses(String args) {
    Matcher matcher = PARENTHESES_PATTERN.matcher(args);
    List<String> listArgs = new ArrayList<>();
    while(matcher.find()) {
      listArgs.add(matcher.group(1));
    }
    return listArgs;
  }

  public static boolean isTheGivenUserAlreadyRegister(User user, DTO.Server server) throws SQLException {
    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());

    return player != null;
  }

  public static User getMentionedUser(List<Member> members) {
    if(members.size() != 1) {
      return null;
    }
    return members.get(0).getUser();
  }
}
