package ch.kalunight.zoe.command.add;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.model.LeagueAccount;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class AddAccountToPlayerCommand extends Command {

  public static final String USAGE_NAME = "accountToPlayer";
  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");
  private static final Logger logger = LoggerFactory.getLogger(AddAccountToPlayerCommand.class);
  
  public AddAccountToPlayerCommand() {
    this.name = USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.arguments = "@MentionPlayer (Region) (accountName)";
    this.userPermissions = permissionRequired;
    this.help = "Add to the mentionned player the given account. Manage Channel permission needed.";
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    event.getTextChannel().sendTyping().complete();

    User user = CreatePlayerCommand.getMentionedUser(event.getMessage().getMentionedMembers());
    if(user == null) {
      event.reply("Please mention 1 member of the server "
          + "(e.g. `>create player @" + event.getMember().getUser().getName() + " (Region) (SummonerName)`)");
      return;
    }
    
    Player player = server.getPlayerByDiscordId(user.getId());
    if(player == null) {
      event.reply("The mentionned user is not registered, please create it first with the command `>create player`.");
      return;
    }

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2) {
      event.reply("The command is malformed. Please respect this pattern : `>create player @DiscordPlayerMention (Region) (SummonerName)`");
      return;
    }
    
    String regionName = listArgs.get(0);
    String summonerName = listArgs.get(1);

    Platform region = CreatePlayerCommand.getPlatform(regionName);
    if(region == null) {
      event.reply("The region tag is invalid. (Valid tag : EUW, EUNE, NA, BR, JP, KR, LAN, LAS, OCE, RU, TR)");
      return;
    }
    
    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName, CallPriority.HIGH);
    }catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.SERVER_ERROR) {
        event.reply("Riot server occured a issue. Please retry");
      }else if(e.getErrorCode() == RiotApiException.UNAVAILABLE) {
        event.reply("Riot server are actually unavailable. Please retry later.");
      }else if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        event.reply("I can't access now to riot server. Please retry later.");
        logger.info("Receive a {} error code : {}", e.getErrorCode(), e.getMessage());
      }else if (e.getErrorCode() == RiotApiException.DATA_NOT_FOUND){
        event.reply("The summonerName is incorrect. Please verify the SummonerName and retry.");
      }else {
        event.reply("I got a unexpected error, please retry. Error code : " + e.getErrorCode());
        logger.warn("Unexpected error in add accountToPlayer command.", e);
      }
      return;
    }
    
    LeagueAccount newAccount = new LeagueAccount(summoner, region);
    
    Player playerAlreadyWithTheAccount = server.isLeagueAccountAlreadyExist(newAccount);
    
    if(playerAlreadyWithTheAccount != null) {
      event.reply("This account is already linked with the player " + playerAlreadyWithTheAccount.getDiscordUser().getName() + " !");
      return;
    }
    
    player.getLolAccounts().add(newAccount);
    event.reply("The account \"" + newAccount.getSummoner().getName() + "\" as been added to the player " + user.getAsMention() + ".");
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Add "+ name +" command :\n");
        stringBuilder.append("--> `>add " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
  
}
