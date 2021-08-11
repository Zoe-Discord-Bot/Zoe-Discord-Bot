package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.BannedAccount;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.BannedAccountRepository;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.AccountVerificationUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import no.stelar7.api.r4j.basic.constants.api.regions.LeagueShard;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.impl.lol.builders.summoner.SummonerBuilder;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class RegisterCommandRunnable {

  public static final String USAGE_NAME = "register";

  private RegisterCommandRunnable() {
    // hide default 
  }

  public static String executeCommand(Server server, Guild guild, Member author, String args,
      EventWaiter waiter, TextChannel channel) throws SQLException {

    ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId, guild.getJDA());

    if(!config.getUserSelfAdding().isOptionActivated()) {
      return String.format(LanguageManager.getText(server.getLanguage(), "registerCommandOptionRequired"),
          LanguageManager.getText(server.getLanguage(), config.getUserSelfAdding().getDescription()));
    }

    User user = author.getUser();

    if(CreatePlayerCommandRunnable.isTheGivenUserAlreadyRegister(user, server)) {
      return LanguageManager.getText(server.getLanguage(), "registerCommandAlreadyInZoe");
    }

    RegionOption regionOption = config.getDefaultRegion();

    List<String> listArgs = CreatePlayerCommandRunnable.getParameterInParenteses(args);
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      return LanguageManager.getText(server.getLanguage(), "registerCommandMalformedWithoutRegionOption");
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      return String.format(LanguageManager.getText(server.getLanguage(), "registerCommandMalformedWithRegionOption"), 
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


    LeagueShard region = CreatePlayerCommandRunnable.getPlatform(regionName);
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
          playerAlreadyWithTheAccount.retrieveUser(guild.getJDA()).getName());
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
        PlayerRepository.createPlayer(server.serv_id, guild.getIdLong(), user.getIdLong(), false);
        DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
        LeagueAccountRepository.createLeagueAccount(player.player_id, summoner, tftSummoner, region.getRealmValue());

        LeagueAccount leagueAccount = 
            LeagueAccountRepository.getLeagueAccountWithSummonerId(server.serv_guildId, summoner.getSummonerId(), region);

        CreatePlayerCommandRunnable.updateLastRank(leagueAccount);

        if(config.getZoeRoleOption().getRole() != null) {
          Member member = guild.retrieveMember(user).complete();
          if(member != null) {
            guild.addRoleToMember(member, config.getZoeRoleOption().getRole()).queue();
          }
        }
        return String.format(LanguageManager.getText(server.getLanguage(), "registerCommandDoneMessage"), summoner.getName());
      }
    }else {
      return LanguageManager.getText(server.getLanguage(), "accountCantBeAddedOwnerChoice");
    }
  }
}
