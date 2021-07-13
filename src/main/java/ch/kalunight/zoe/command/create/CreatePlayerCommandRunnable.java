package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import ch.kalunight.zoe.util.LastRankUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.endpoints.tft_league.dto.TFTLeagueEntry;
import net.rithms.riot.api.endpoints.tft_summoner.dto.TFTSummoner;
import net.rithms.riot.constant.Platform;

public class CreatePlayerCommandRunnable {

  private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");
  
  private CreatePlayerCommandRunnable() {
    // hide default public constructor
  }

  public static String executeCommand(Server server, JDA jda, Member author, List<Member> mentionnedMembers, String args) throws SQLException {
    
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
          regionOption.getRegion().getName().toUpperCase());
    }

    String regionName;
    String summonerName;
    if(listArgs.size() == 2) {
      regionName = listArgs.get(0);
      summonerName = listArgs.get(1);
    }else {
      regionName = regionOption.getRegion().getName();
      summonerName = listArgs.get(0);
    }


    Platform region = getPlatform(regionName);
    if(region == null) {
      return LanguageManager.getText(server.getLanguage(), "regionTagInvalid");
    }

    Summoner summoner;
    TFTSummoner tftSummoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByNameWithRateLimit(region, summonerName);
      tftSummoner = Zoe.getRiotApi().getTFTSummonerByNameWithRateLimit(region, summonerName);
    }catch(RiotApiException e) {
      return RiotApiUtil.getTextHandlerRiotApiError(e, server.getLanguage());
    }

    DTO.Player playerAlreadyWithTheAccount = PlayerRepository
        .getPlayerByLeagueAccountAndGuild(server.serv_guildId, summoner.getId(), region);

    if(playerAlreadyWithTheAccount != null) {
      return String.format(LanguageManager.getText(server.getLanguage(), "accountAlreadyLinkedToAnotherPlayer"),
          playerAlreadyWithTheAccount.retrieveUser(jda).getName());
    }

    BannedAccount bannedAccount = BannedAccountRepository.getBannedAccount(summoner.getId(), region);
    if(bannedAccount == null) {

      PlayerRepository.createPlayer(server.serv_id, server.serv_guildId, user.getIdLong(), false);
      DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
      LeagueAccountRepository.createLeagueAccount(player.player_id, summoner, tftSummoner, region.getName());
      
      LeagueAccount leagueAccount = 
          LeagueAccountRepository.getLeagueAccountWithSummonerId(server.serv_guildId, summoner.getId(), region);
      
      updateLastRank(leagueAccount);

      if(config.getZoeRoleOption().getRole() != null) {
        Member member = author.getGuild().retrieveMember(user).complete();
        if(member != null) {
          author.getGuild().addRoleToMember(member, config.getZoeRoleOption().getRole()).queue();
        }
      }

      return String.format(LanguageManager.getText(server.getLanguage(), "createPlayerDoneMessage"),
          user.getName(), summoner.getName());
    }else {
      return LanguageManager.getText(server.getLanguage(), "accountCantBeAddedOwnerChoice");
    }
  }

  public static void updateLastRank(LeagueAccount leagueAccount) throws SQLException {
    LastRankRepository.createLastRank(leagueAccount.leagueAccount_id);
    LastRank lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);
    
    try {
      Set<LeagueEntry> leagueEntries = Zoe.getRiotApi().
          getLeagueEntriesBySummonerId(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);
      LastRankUtil.updateLoLLastRank(lastRank, leagueEntries);
    } catch(RiotApiException e) {
      Zoe.logger.info("Fail to refresh LoL last rank while creating a leagueAccount, will be done at the next game.");
    }
    
    try {
      Set<TFTLeagueEntry> tftLeagueEntries = Zoe.getRiotApi().
          getTFTLeagueEntries(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_tftSummonerId);
      LastRankUtil.updateTFTLastRank(lastRank, tftLeagueEntries);
    } catch(RiotApiException e) {
      Zoe.logger.info("Fail to refresh TFT last rank while creating a leagueAccount, will be done at the next game.");
    }
  }

  public static Platform getPlatform(String regionName) {
    Platform region;
    try {
      region = Platform.getPlatformByName(regionName);
    } catch(NoSuchElementException e) {
      return null;
    }
    return region;
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
