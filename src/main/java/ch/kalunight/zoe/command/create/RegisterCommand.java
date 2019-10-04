package ch.kalunight.zoe.command.create;

import java.util.List;
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
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class RegisterCommand extends Command {

  public static final String USAGE_NAME = "register";
  
  private static final Logger logger = LoggerFactory.getLogger(RegisterCommand.class);

  public RegisterCommand() {
    this.name = USAGE_NAME;
    this.arguments = "(Region) (SummonerName)";
    this.help = "registerCommandHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethod(USAGE_NAME, help);
  }
  
  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(!server.getConfig().getUserSelfAdding().isOptionActivated()) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "registerCommandOptionRequired"),
          server.getConfig().getUserSelfAdding().getDescription()));
      return;
    }
    
    User user = event.getAuthor();
    
    if(CreatePlayerCommand.isTheGivenUserAlreadyRegister(user, server)) {
      event.reply(LanguageManager.getText(server.getLangage(), "registerCommandAlreadyInZoe"));
      return;
    }
    
    RegionOption regionOption = server.getConfig().getDefaultRegion();
    
    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "registerCommandMalformedWithoutRegionOption"));
      return;
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "registerCommandMalformedWithRegionOption"), 
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
    
    Player playerAlreadyWithTheAccount = server.isLeagueAccountAlreadyExist(new LeagueAccount(summoner, region));
    
    if(playerAlreadyWithTheAccount != null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "accountAlreadyLinkedToAnotherPlayer"),
          playerAlreadyWithTheAccount.getDiscordUser().getName()));
      return;
    }
    
    Player player = new Player(user, summoner, region, false);
    server.getPlayers().add(player);
    if(server.getConfig().getZoeRoleOption().getRole() != null) {
      Member member = server.getGuild().getMember(user);
      if(member != null) {
        server.getGuild().addRoleToMember(member, server.getConfig().getZoeRoleOption().getRole()).queue();
      }
    }
    event.reply(String.format(LanguageManager.getText(server.getLangage(), "registerCommandDoneMessage"), summoner.getName()));
  }
}
