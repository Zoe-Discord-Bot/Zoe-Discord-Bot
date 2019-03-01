package ch.kalunight.zoe.command;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.ZoeMain;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class CreatePlayerCommand extends Command{

  private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");
  
  private static final Logger logger = LoggerFactory.getLogger(CreatePlayerCommand.class);

  public CreatePlayerCommand() {
    this.name = "player";
    this.help = "Create a player with the given information";
    this.arguments = "*@DiscordMentionOfPlayer* (*Region*) (*SummonerName*)";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
  }

  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().queue();
    List<Member> members = event.getMessage().getMentionedMembers();

    if(members.size() != 1) {
      event.reply("Please mention 1 member of the server (e.g. `>create player " + event.getMember().getAsMention() + "\" (Region) (SummonerName)`");
      return;
    }

    User user = members.get(0).getUser();

    Matcher matcher = PARENTHESES_PATTERN.matcher(event.getArgs());
    if(matcher.groupCount() != 2) {
      event.reply("The command is malformed. Please respect this pattern : `>create player *@DiscordPlayerMention* (*Region*) (*SummonerName*)`");
      return;
    }

    String regionName = matcher.group(1);
    String summonerName = matcher.group(2);

    Platform region;
    try {
      region = Platform.getPlatformByName(regionName);
    } catch(NoSuchElementException e) {
      event.reply("The region tag is invalid. (Valid tag : EUW, EUNE, NA, BR, JP, KR, LAN, LAS, OCE, RU, TR");
      return;
    }

    Summoner summoner;
    try {
      summoner = ZoeMain.getRiotApi().getSummonerByName(region, summonerName);
    }catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.SERVER_ERROR || e.getErrorCode() == RiotApiException.UNAVAILABLE) {
        event.reply("Riot server occured a issue, please retry later");
      }else if(e.getErrorCode() == RiotApiException.FORBIDDEN || e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        event.reply("I have some little problem with my acces to riot server. Please retry later.");
        logger.warn("Receive a {} error code : {}", e.getErrorCode(), e.getMessage());
      }else {
        event.reply("The summonerName is incorrect. Please verify the SummonerName and retry.");
      }
      return;
    }
    
    Player player = new Player(user, summoner, region, false);
    Server server = ServerData.getServers().get(event.getGuild().getId());
    server.getPlayers().add(player);
    event.reply("The player " + user.getName() + " has been added with the account \"" + player.getSummoner() + "\".");
  }

}
