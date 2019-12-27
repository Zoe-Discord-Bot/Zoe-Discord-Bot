package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class RegisterCommand extends ZoeCommand {

  public static final String USAGE_NAME = "register";
  
  private static final Logger logger = LoggerFactory.getLogger(RegisterCommand.class);

  public RegisterCommand() {
    this.name = USAGE_NAME;
    this.arguments = "(Region) (SummonerName)";
    this.help = "registerCommandHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethod(USAGE_NAME, help);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    event.getTextChannel().sendTyping().complete();
    
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId);
    
    if(!config.getUserSelfAdding().isOptionActivated()) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "registerCommandOptionRequired"),
          LanguageManager.getText(server.serv_language, config.getUserSelfAdding().getDescription())));
      return;
    }
    
    User user = event.getAuthor();
    
    if(CreatePlayerCommand.isTheGivenUserAlreadyRegister(user, server)) {
      event.reply(LanguageManager.getText(server.serv_language, "registerCommandAlreadyInZoe"));
      return;
    }
    
    RegionOption regionOption = config.getDefaultRegion();
    
    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      event.reply(LanguageManager.getText(server.serv_language, "registerCommandMalformedWithoutRegionOption"));
      return;
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "registerCommandMalformedWithRegionOption"), 
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
      event.reply(LanguageManager.getText(server.serv_language, "regionTagInvalid"));
      return;
    }

    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
    }catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.SERVER_ERROR) {
        event.reply(LanguageManager.getText(server.serv_language, "riotApiSummonerByNameError500"));
      }else if(e.getErrorCode() == RiotApiException.UNAVAILABLE) {
        event.reply(LanguageManager.getText(server.serv_language, "riotApiSummonerByNameError503"));
      }else if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        event.reply(LanguageManager.getText(server.serv_language, "riotApiSummonerByNameError429"));
        logger.info("Receive a {} error code : {}", e.getErrorCode(), e.getMessage());
      }else if (e.getErrorCode() == RiotApiException.DATA_NOT_FOUND){
        event.reply(LanguageManager.getText(server.serv_language, "riotApiSummonerByNameError404"));
      }else {
        event.reply(String.format(LanguageManager.getText(server.serv_language, "riotApiSummonerByNameErrorUnexpected"), e.getErrorCode()));
        logger.warn("Unexpected error in add accountToPlayer command.", e);
      }
      return;
    }
    
    DTO.Player playerAlreadyWithTheAccount = PlayerRepository
        .getPlayerByLeagueAccountAndGuild(server.serv_guildId, summoner.getId(), region.getName());
    
    if(playerAlreadyWithTheAccount != null) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "accountAlreadyLinkedToAnotherPlayer"),
          playerAlreadyWithTheAccount.user.getName()));
      return;
    }
    
    PlayerRepository.createPlayer(server.serv_id, user.getIdLong(), false);
    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
    LeagueAccountRepository.createLeagueAccount(player.player_id, summoner, region.getName());
    
    if(config.getZoeRoleOption().getRole() != null) {
      Member member = event.getGuild().getMember(user);
      if(member != null) {
        event.getGuild().addRoleToMember(member, config.getZoeRoleOption().getRole()).queue();
      }
    }
    event.reply(String.format(LanguageManager.getText(server.serv_language, "registerCommandDoneMessage"), summoner.getName()));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
