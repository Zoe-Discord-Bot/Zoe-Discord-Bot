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
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class AddAccountCommand extends Command {

  public static final String USAGE_NAME = "account";
  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");
  private static final Logger logger = LoggerFactory.getLogger(AddAccountCommand.class);

  public AddAccountCommand() {
    this.name = USAGE_NAME;
    String[] aliases = {"accountToPlayers", "accountsToPlayers", "accountToPlayers", "accountToPlayer", "accounts"};
    this.aliases = aliases;
    this.arguments = "@MentionPlayer (Region) (accountName)";
    this.help = "Add to the mentionned player the given account.";
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();

    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(!server.getConfig().getUserSelfAdding().isOptionActivated() && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
        event.reply("You need the permission \"" + Permission.MANAGE_CHANNEL.getName() + "\" to do that.");
        return;
    }
    
    User user = CreatePlayerCommand.getMentionedUser(event.getMessage().getMentionedMembers());
    if(user == null) {
      event.reply("Please mention 1 member of the server "
          + "(e.g. `>create player @" + event.getMember().getUser().getName() + " (Region) (SummonerName)`)");
      return;
    }else if(!user.equals(event.getAuthor()) && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      event.reply("You cannot add an account to another player than you if don't have the *" + Permission.MANAGE_CHANNEL.getName() + "* permission. "
          + "Sorry about that :/");
      return;
    }

    Player player = server.getPlayerByDiscordId(user.getId());
    if(player == null) {
      event.reply("The mentionned user is not registered, please create it first with the command `>create player`.");
      return;
    }

    RegionOption regionOption = server.getConfig().getDefaultRegion();

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      event.reply("The command is malformed. Please respect this pattern : `>add " + name + " @DiscordPlayerMention (Region) (SummonerName)`");
      return;
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      event.reply("The command is malformed. Please respect this pattern : `>add " + name + " @DiscordPlayerMention (Region) (SummonerName)`\n"
          + "If the region is not set, the default region will be used (" + regionOption.getRegion().getName().toUpperCase() + ").");
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
    event.reply("The account \"" + newAccount.getSummoner().getName() + "\" has been added to the player " + user.getName() + ".");
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
