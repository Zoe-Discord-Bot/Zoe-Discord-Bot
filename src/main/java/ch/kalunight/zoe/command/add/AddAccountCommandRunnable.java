package ch.kalunight.zoe.command.add;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;
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
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.endpoints.tft_summoner.dto.TFTSummoner;
import net.rithms.riot.constant.Platform;

public class AddAccountCommandRunnable {

  public static final String USAGE_NAME = "account";
  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");

  private AddAccountCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(Server server, Member author, List<Member> mentionnedMembers, String args) throws SQLException {

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
          USAGE_NAME, regionOption.getRegion().getName().toUpperCase());
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

    Platform region = CreatePlayerCommandRunnable.getPlatform(regionName);
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

    LeagueAccount newAccount = new LeagueAccount(summoner, region);

    DTO.Player playerAlreadyWithTheAccount = PlayerRepository
        .getPlayerByLeagueAccountAndGuild(server.serv_guildId, summoner.getId(), region);

    if(playerAlreadyWithTheAccount != null) {
      User userAlreadyWithTheAccount = author.getJDA().retrieveUserById(playerAlreadyWithTheAccount.player_discordId).complete();
      return String.format(LanguageManager.getText(server.getLanguage(), "accountAlreadyLinkedToAnotherPlayer"), 
          userAlreadyWithTheAccount.getName());
    }

    BannedAccount bannedAccount = BannedAccountRepository.getBannedAccount(summoner.getId(), region);
    if(bannedAccount == null) {
      
      LeagueAccountRepository.createLeagueAccount(player.player_id, summoner, tftSummoner, region.getName());
      DTO.LeagueAccount leagueAccount = LeagueAccountRepository.getLeagueAccountWithSummonerId(server.serv_guildId, summoner.getId(), region);
      
      CreatePlayerCommandRunnable.updateLastRank(leagueAccount);
      
      return String.format(LanguageManager.getText(server.getLanguage(), "accountAddedToPlayer"),
          newAccount.getSummoner().getName(), user.getName());
    }else {
      return LanguageManager.getText(server.getLanguage(), "accountCantBeAddedOwnerChoice");
    }
  }

}
