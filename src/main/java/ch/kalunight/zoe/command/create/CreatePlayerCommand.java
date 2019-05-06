package ch.kalunight.zoe.command.create;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class CreatePlayerCommand extends Command{

  public static final String USAGE_NAME = "player";

  private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");

  private static final Logger logger = LoggerFactory.getLogger(CreatePlayerCommand.class);

  public CreatePlayerCommand() {
    this.name = USAGE_NAME;
    this.help = "Create a player with the given information. Manage Channel permission needed.";
    this.arguments = "@DiscordMentionOfPlayer (Region) (SummonerName)";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  public void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();

    User user = getMentionedUser(event.getMessage().getMentionedMembers());
    if(user == null) {
      event.reply("Please mention 1 member of the server "
          + "(e.g. `>create player @" + event.getMember().getUser().getName() + " (Region) (SummonerName)`)");
      return;
    }

    Server server = ServerData.getServers().get(event.getGuild().getId());
    if(isTheGivenUserAlreadyRegister(user, server)) {
      event.reply("The mentioned member is already register. If you want to modify the LoL account please delete it first.");
      return;
    }

    List<String> listArgs = getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2) {
      event.reply("The command is malformed. Please respect this pattern : `>create player @DiscordPlayerMention (Region) (SummonerName)`");
      return;
    }
    
    String regionName = listArgs.get(0);
    String summonerName = listArgs.get(1);

    Platform region = getPlatform(regionName);
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
      }else {
        event.reply("The summonerName is incorrect. Please verify the SummonerName and retry.");
      }
      return;
    }

    Player player = new Player(user, summoner, region, false);
    server.getPlayers().add(player);
    event.reply("The player " + user.getName() + " has been added with the account \"" + player.getSummoner().getName() + "\".");
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
    return server.getPlayerByDiscordId(user.getId()) != null;
  }

  public static User getMentionedUser(List<Member> members) {
    if(members.size() != 1) {
      return null;
    }
    return members.get(0).getUser();
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Create player command :\n");
        stringBuilder.append("--> `>create " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
}
