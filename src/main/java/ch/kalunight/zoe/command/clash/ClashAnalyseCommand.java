package ch.kalunight.zoe.command.clash;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.command.show.ShowCommand;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.service.clashchannel.LoadClashTeamAndStartBanAnalyseWorker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.entities.Message;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class ClashAnalyseCommand extends ZoeCommand {
  
  public static final String USAGE_NAME = "analysis";
  
  public ClashAnalyseCommand() {
    this.name = USAGE_NAME;
    String[] aliases = {"stats"};
    this.arguments = "(Platform) (Summoner Name)";
    this.aliases = aliases;
    this.help = "clashAnalyzeHelpMessage";
    this.cooldown = 30;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(ShowCommand.USAGE_NAME, name, arguments, help);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    ClashChannel channelToTreat = null;
    List<ClashChannel> clashchannls = ClashChannelRepository.getClashChannels(server.serv_guildId);
    for(ClashChannel channelToCheck : clashchannls) {
      if(channelToCheck.clashChannel_channelId == event.getChannel().getIdLong()) {
        channelToTreat = channelToCheck;
        break;
      }
    }
    
    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    String regionName;
    String summonerName;
    
    if(channelToTreat == null) {
      if(listArgs.size() == 2) {
        regionName = listArgs.get(0);
        summonerName = listArgs.get(1);
      }else {
        event.reply(LanguageManager.getText(server.getLanguage(), "clashAnalyzeMalformedFormat"));
        return;
      }
      
    }else {
      
      if(listArgs.size() == 2) {
        regionName = listArgs.get(0);
        summonerName = listArgs.get(1);
      }else if (listArgs.size() == 1){
        summonerName = listArgs.get(0);
        regionName = channelToTreat.clashChannel_data.getSelectedPlatform().getName();
      }else {
        event.reply(String.format(LanguageManager.getText(server.getLanguage(), "clashAnalyzeMalformedFormatInsideClashChannel"), 
            channelToTreat.clashChannel_data.getSelectedPlatform().getName().toUpperCase()));
        return;
      }
      
    }
    
    Message loadingMessage = event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "loadingSummoner")).complete();
    Platform platorm = CreatePlayerCommand.getPlatform(regionName);
    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByNameWithRateLimit(platorm, summonerName);
    }catch(RiotApiException e) {
      RiotApiUtil.handleRiotApi(loadingMessage, e, server.getLanguage());
      return;
    }
    
    if(summoner != null) {
      LoadClashTeamAndStartBanAnalyseWorker loadClashTeamWorker = new LoadClashTeamAndStartBanAnalyseWorker(server, summoner.getId(), platorm, event.getTextChannel(), channelToTreat);
      loadingMessage.editMessage("*" + LanguageManager.getText(server.getLanguage(), "clashAnalyzeLoadStarted") + "*").queue();
      ServerThreadsManager.getClashChannelExecutor().execute(loadClashTeamWorker);
    }
    
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
