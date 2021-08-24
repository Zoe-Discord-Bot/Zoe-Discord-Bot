package ch.kalunight.zoe.command.remove;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreatePlayerCommandRunnable;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class RemoveAccountCommandRunnable {

  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");
  public static final String USAGE_NAME = "account";
  
  private RemoveAccountCommandRunnable() {
    // hide default public constructor
  }

  public static String executeCommand(Server server, Member author, List<Member> mentionnedMembers, String args) throws SQLException {

    ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId, author.getJDA());

    if(!config.getUserSelfAdding().isOptionActivated() &&
        !author.getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      return String.format(LanguageManager.getText(server.getLanguage(), "deletePlayerMissingPermission"),
          Permission.MANAGE_CHANNEL.getName());
    }

    User user = CreatePlayerCommandRunnable.getMentionedUser(mentionnedMembers);
    if(user == null) {
      return String.format(LanguageManager.getText(server.getLanguage(), "removeAccountMissingMention"),
          author.getUser().getName());
    } else if(!user.getId().equals(author.getUser().getId()) && !author.getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      return String.format(LanguageManager.getText(server.getLanguage(), "removeAccountMissingRight"),
          Permission.MANAGE_CHANNEL.getName());
    }

    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
    if(player == null) {
      return LanguageManager.getText(server.getLanguage(), "removeAccountUserNotRegistered");
    }

    List<String> listArgs = CreatePlayerCommandRunnable.getParameterInParenteses(args);
    if(listArgs.size() != 2) {
      return LanguageManager.getText(server.getLanguage(), "removeAccountMalformed");
    }

    String regionName = listArgs.get(0);
    String summonerName = listArgs.get(1);

    ZoePlatform region = CreatePlayerCommandRunnable.getPlatform(regionName);
    if(region == null) {
      return LanguageManager.getText(server.getLanguage(), "regionTagInvalid");
    }

    DTO.LeagueAccount account;
    try {
      Summoner summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
      
      account = LeagueAccountRepository
          .getLeagueAccountWithSummonerId(server.serv_guildId, summoner.getSummonerId(), region);
    } catch(APIResponseException e) {
      return RiotApiUtil.getTextHandlerRiotApiError(e, server.getLanguage());
    }

    if(account == null || account.leagueAccount_fk_player != player.player_id) {
      return LanguageManager.getText(server.getLanguage(), "removeAccountNotLinkedToPlayer");
    }
    
    LeagueAccountRepository.deleteAccountWithId(account.leagueAccount_id);
    return String.format(LanguageManager.getText(server.getLanguage(), "removeAccountDoneMessage"),
        summonerName, user.getName());
  }
}
