package ch.kalunight.zoe.command.stats;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.TeamUtil;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class PredictRoleCommand extends ZoeCommand {

  private class LeagueAccount { 
    public Summoner summoner;
    public Platform platform;
  }

  public PredictRoleCommand() {
    this.name = "predictRole";
    String[] aliases = {"role", "predictPosition", "predict"};
    this.aliases = aliases;
    this.arguments = "(Region1) (summonerName1) (Region2) (summonerName2) (Region3) (summonerName3) (Region4) (summonerName4) (Region5) (summonerName5) "
        + "Or with region option : (summonerName1) (summonerName2) (summonerName3) (summonerName4) (summonerName5) ";
    this.help = "statsPredictRoleHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(StatsCommand.USAGE_NAME, name, arguments, help);
    this.cooldown = 60;
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    DTO.Server server = getServer(event.getGuild().getIdLong());

    ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId);

    RegionOption regionOption = config.getDefaultRegion();

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 10 && regionOption.getRegion() == null) {
      event.reply(LanguageManager.getText(server.getLanguage(), "createPlayerMalformedWithoutRegionOption"));
      return;
    }else if(listArgs.size() != 10 && listArgs.size() != 5 && regionOption.getRegion() != null) {
      event.reply(String.format(LanguageManager.getText(server.getLanguage(), "createPlayerMalformedWithRegionOption"), 
          regionOption.getRegion().getName().toUpperCase()));
      return;
    }

    List<LeagueAccount> leagueAccountToTreat = new ArrayList<>();
    int playerToTreath = 5;
    int currentlySelected = 0;
    while(playerToTreath > 0) {

      String regionName;
      String summonerName;
      if(listArgs.size() == 10) {
        regionName = listArgs.get(currentlySelected);
        summonerName = listArgs.get(currentlySelected + 1);
        currentlySelected += 2;
      }else {
        regionName = regionOption.getRegion().getName();
        summonerName = listArgs.get(0);
        currentlySelected++;
      }

      Platform region;
      if(listArgs.size() == 10) {
        region = CreatePlayerCommand.getPlatform(regionName);
        if(region == null) {
          event.reply(LanguageManager.getText(server.getLanguage(), "regionTagInvalid"));
          return;
        }
      }else {
        region = config.getDefaultRegion().getRegion();
      }

      try {
        LeagueAccount leagueAccount = new LeagueAccount();
        leagueAccount.summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
        leagueAccount.platform = region;
        leagueAccountToTreat.add(leagueAccount);
      }catch(RiotApiException e) {
        RiotApiUtil.handleRiotApi(event.getEvent(), e, server.getLanguage());
        return;
      }
      playerToTreath--;
    }

    if(leagueAccountToTreat.size() == 5) {

      List<TeamPlayerAnalysisDataCollector> accountAnalyser = new ArrayList<>();

      for(LeagueAccount leagueAccount : leagueAccountToTreat) {
        TeamPlayerAnalysisDataCollector accountToAnalyse = new TeamPlayerAnalysisDataCollector(leagueAccount.summoner.getId(), leagueAccount.platform, null);
        ServerThreadsManager.getDataAnalysisThread().execute(accountToAnalyse);
        accountAnalyser.add(accountToAnalyse);
      }

      TeamPlayerAnalysisDataCollector.awaitAll(accountAnalyser);      

      TeamUtil.determineRole(accountAnalyser);
      
      Collections.sort(accountAnalyser);

      StringBuilder builder = new StringBuilder();
      builder.append(LanguageManager.getText(server.getLanguage(), "statsPredictRoleTitleDeterminedRole") + "\n");
      for(TeamPlayerAnalysisDataCollector playerToShow : accountAnalyser) {
        builder.append(LanguageManager.getText(server.getLanguage(), TeamUtil.getChampionRoleAbrID(playerToShow.getFinalDeterminedPosition())) + " : *" 
            + playerToShow.getPlatform().getName().toUpperCase() + "* " + playerToShow.getSummoner().getSumCacheData().getName() + "\n");
      }

      builder.append("*" + LanguageManager.getText(server.getLanguage(), "disclaimerAnalysis") + "*");
      
      event.reply(builder.toString());
      
    }else {
      //error
    }

  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
