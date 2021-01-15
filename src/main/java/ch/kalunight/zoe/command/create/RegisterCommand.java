package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.BannedAccount;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.repositories.BannedAccountRepository;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.endpoints.tft_summoner.dto.TFTSummoner;
import net.rithms.riot.constant.Platform;

public class RegisterCommand extends ZoeCommand {

  public static final String USAGE_NAME = "register";

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
      event.reply(String.format(LanguageManager.getText(server.getLanguage(), "registerCommandOptionRequired"),
          LanguageManager.getText(server.getLanguage(), config.getUserSelfAdding().getDescription())));
      return;
    }

    User user = event.getAuthor();

    if(CreatePlayerCommand.isTheGivenUserAlreadyRegister(user, server)) {
      event.reply(LanguageManager.getText(server.getLanguage(), "registerCommandAlreadyInZoe"));
      return;
    }

    RegionOption regionOption = config.getDefaultRegion();

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      event.reply(LanguageManager.getText(server.getLanguage(), "registerCommandMalformedWithoutRegionOption"));
      return;
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      event.reply(String.format(LanguageManager.getText(server.getLanguage(), "registerCommandMalformedWithRegionOption"), 
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
      event.reply(LanguageManager.getText(server.getLanguage(), "regionTagInvalid"));
      return;
    }

    Summoner summoner;
    TFTSummoner tftSummoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
      tftSummoner = Zoe.getRiotApi().getTFTSummonerByName(region, summonerName);
    }catch(RiotApiException e) {
      RiotApiUtil.handleRiotApi(event.getEvent(), e, server.getLanguage());
      return;
    }

    DTO.Player playerAlreadyWithTheAccount = PlayerRepository
        .getPlayerByLeagueAccountAndGuild(server.serv_guildId, summoner.getId(), region);

    if(playerAlreadyWithTheAccount != null) {
      event.reply(String.format(LanguageManager.getText(server.getLanguage(), "accountAlreadyLinkedToAnotherPlayer"),
          playerAlreadyWithTheAccount.getUser().getName()));
      return;
    }

    BannedAccount bannedAccount = BannedAccountRepository.getBannedAccount(summoner.getId(), region);
    if(bannedAccount == null) {

      PlayerRepository.createPlayer(server.serv_id, event.getGuild().getIdLong(), user.getIdLong(), false);
      DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
      LeagueAccountRepository.createLeagueAccount(player.player_id, summoner, tftSummoner, region.getName());

      LeagueAccount leagueAccount = 
          LeagueAccountRepository.getLeagueAccountWithSummonerId(server.serv_guildId, summoner.getId(), region);
      
      CreatePlayerCommand.updateLastRank(leagueAccount);
      
      if(config.getZoeRoleOption().getRole() != null) {
        Member member = event.getGuild().retrieveMember(user).complete();
        if(member != null) {
          event.getGuild().addRoleToMember(member, config.getZoeRoleOption().getRole()).queue();
        }
      }
      event.reply(String.format(LanguageManager.getText(server.getLanguage(), "registerCommandDoneMessage"), summoner.getName()));

    }else {
      event.reply(LanguageManager.getText(server.getLanguage(), "accountCantBeAddedOwnerChoice"));
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
