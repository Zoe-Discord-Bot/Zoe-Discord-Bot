package ch.kalunight.zoe.command.add;

import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class AddAccountCommand extends ZoeCommand {

  public static final String USAGE_NAME = "account";
  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");
  private static final Logger logger = LoggerFactory.getLogger(AddAccountCommand.class);

  public AddAccountCommand() {
    this.name = USAGE_NAME;
    String[] aliases = {"accountToPlayers", "accountsToPlayers", "accountToPlayers", "accountToPlayer", "accounts"};
    this.aliases = aliases;
    this.arguments = "@MentionPlayer (Region) (accountName)";
    this.help = "addAccountHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(AddCommand.USAGE_NAME, USAGE_NAME, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();

    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(!server.getConfig().getUserSelfAdding().isOptionActivated() && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
        event.reply(String.format(LanguageManager.getText(server.getLangage(), "permissionNeededMessage"),
            Permission.MANAGE_CHANNEL.getName()));
        return;
    }
    
    User user = CreatePlayerCommand.getMentionedUser(event.getMessage().getMentionedMembers());
    if(user == null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "mentionNeededMessageWithUser"),
          event.getMember().getUser().getName()));
      return;
    }else if(!user.equals(event.getAuthor()) && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      event.reply(LanguageManager.getText(server.getLangage(), "permissionNeededUpdateOtherPlayer"));
      return;
    }

    Player player = server.getPlayerByDiscordId(user.getIdLong());
    if(player == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "mentionnedUserNotRegistered"));
      return;
    }

    RegionOption regionOption = server.getConfig().getDefaultRegion();

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "addCommandMalformedWithoutRegionOption"), name));
      return;
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "addCommandMalformedWithRegionOption"),
          name, regionOption.getRegion().getName().toUpperCase()));
      return;
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

    Platform region = CreatePlayerCommand.getPlatform(regionName);
    if(region == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "regionTagInvalid"));
      return;
    }

    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName, CallPriority.HIGH);
    }catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.SERVER_ERROR) {
        event.reply(LanguageManager.getText(server.getLangage(), "riotApiSummonerByNameError500"));
      }else if(e.getErrorCode() == RiotApiException.UNAVAILABLE) {
        event.reply(LanguageManager.getText(server.getLangage(), "riotApiSummonerByNameError503"));
      }else if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        event.reply(LanguageManager.getText(server.getLangage(), "riotApiSummonerByNameError429"));
        logger.info("Receive a {} error code : {}", e.getErrorCode(), e.getMessage());
      }else if (e.getErrorCode() == RiotApiException.DATA_NOT_FOUND){
        event.reply(LanguageManager.getText(server.getLangage(), "riotApiSummonerByNameError404"));
      }else {
        event.reply(String.format(LanguageManager.getText(server.getLangage(), "riotApiSummonerByNameErrorUnexpected"), e.getErrorCode()));
        logger.warn("Unexpected error in add accountToPlayer command.", e);
      }
      return;
    }

    LeagueAccount newAccount = new LeagueAccount(summoner, region);

    Player playerAlreadyWithTheAccount = server.isLeagueAccountAlreadyExist(newAccount);

    if(playerAlreadyWithTheAccount != null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "accountAlreadyLinkedToAnotherPlayer"), 
          playerAlreadyWithTheAccount.getDiscordUser().getName()));
      return;
    }

    player.getLolAccounts().add(newAccount);
    event.reply(String.format(LanguageManager.getText(server.getLangage(), "accountAddedToPlayer"),
        newAccount.getSummoner().getName(), user.getName()));
  }

}
