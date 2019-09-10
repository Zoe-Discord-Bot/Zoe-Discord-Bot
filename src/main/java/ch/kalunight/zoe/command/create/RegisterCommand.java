package ch.kalunight.zoe.command.create;

import java.util.List;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class RegisterCommand extends Command {

  public static final String USAGE_NAME = "register";
  
  private static final Logger logger = LoggerFactory.getLogger(RegisterCommand.class);

  public RegisterCommand() {
    this.name = USAGE_NAME;
    this.help = "Register the writer with the given information. "
        + "Only enable when the option **Everyone can add/delete them self in the system** is activated.";
    this.arguments = "(Region) (SummonerName)";
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(!server.getConfig().getUserSelfAdding().isOptionActivated()) {
      event.reply("This command can only be used when the option \"" + server.getConfig().getUserSelfAdding().getDescription() + "\" is enable.");
      return;
    }
    
    User user = event.getAuthor();
    
    if(CreatePlayerCommand.isTheGivenUserAlreadyRegister(user, server)) {
      event.reply("You are already in the system. "
          + "If you want to add another lol account, please use the command ``>add accountToPlayer``.");
      return;
    }
    
    RegionOption regionOption = server.getConfig().getDefaultRegion();
    
    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      event.reply("The command is malformed. Please respect this pattern : `>register (Region) (SummonerName)`");
      return;
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      event.reply("The command is malformed. Please respect this pattern : `>register (Region) (SummonerName)`\n"
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
    
    Player playerAlreadyWithTheAccount = server.isLeagueAccountAlreadyExist(new LeagueAccount(summoner, region));
    
    if(playerAlreadyWithTheAccount != null) {
      event.reply("This account is already linked with the player " + playerAlreadyWithTheAccount.getDiscordUser().getName() + " !");
      return;
    }
    
    Player player = new Player(user, summoner, region, false);
    server.getPlayers().add(player);
    if(server.getConfig().getZoeRoleOption().getRole() != null) {
      Member member = server.getGuild().getMember(user);
      if(member != null) {
        server.getGuild().getController().addRolesToMember(member, server.getConfig().getZoeRoleOption().getRole()).queue();
      }
    }
    event.reply("You have been added with the account \"" + summoner.getName() + "\".");
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Register command :\n");
        stringBuilder.append("--> `>"+ name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
}
