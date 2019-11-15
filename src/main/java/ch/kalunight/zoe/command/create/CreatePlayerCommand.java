package ch.kalunight.zoe.command.create;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class CreatePlayerCommand extends ZoeCommand {

  public static final String USAGE_NAME = "player";

  private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");

  private static final Logger logger = LoggerFactory.getLogger(CreatePlayerCommand.class);

  public CreatePlayerCommand() {
    this.name = USAGE_NAME;
    this.help = "createPlayerHelpMessage";
    this.arguments = "@DiscordMentionOfPlayer (Region) (SummonerName)";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  public void executeCommand(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(!server.getConfig().getUserSelfAdding().isOptionActivated() && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "permissionNeededMessage"),
          Permission.MANAGE_CHANNEL.getName()));
        return;
    }
    
    User user = getMentionedUser(event.getMessage().getMentionedMembers());
    if(user == null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "mentionNeededMessageWithUser"), event.getAuthor().getName()));
      return;
    }else if(!user.equals(event.getAuthor()) && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "permissionNeededCreateOtherPlayer"),
          Permission.MANAGE_CHANNEL.getName()));
      return;
    }

    if(isTheGivenUserAlreadyRegister(user, server)) {
      event.reply(LanguageManager.getText(server.getLangage(), "createPlayerAlreadyRegistered"));
      return;
    }
    
    RegionOption regionOption = server.getConfig().getDefaultRegion();

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "createPlayerMalformedWithoutRegionOption"));
      return;
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "createPlayerMalformedWithRegionOption"), 
          regionOption.getRegion().getName().toUpperCase()));
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


    Platform region = getPlatform(regionName);
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
    
    Player playerAlreadyWithTheAccount = server.isLeagueAccountAlreadyExist(new LeagueAccount(summoner, region));
    
    if(playerAlreadyWithTheAccount != null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "accountAlreadyLinkedToAnotherPlayer"),
          playerAlreadyWithTheAccount.getDiscordUser().getName()));
      return;
    }

    Player player = new Player(user.getIdLong(), summoner, region, false);
    server.getPlayers().add(player);
    if(server.getConfig().getZoeRoleOption().getRole() != null) {
      Member member = server.getGuild().getMember(user);
      if(member != null) {
        server.getGuild().addRoleToMember(member, server.getConfig().getZoeRoleOption().getRole()).queue();
      }
    }
    event.reply(String.format(LanguageManager.getText(server.getLangage(), "createPlayerDoneMessage"),
        user.getName(), summoner.getName()));
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

  public static boolean isTheGivenUserAlreadyRegister(User user, Server server) {
    return server.getPlayerByDiscordId(user.getIdLong()) != null;
  }

  public static User getMentionedUser(List<Member> members) {
    if(members.size() != 1) {
      return null;
    }
    return members.get(0).getUser();
  }
}
