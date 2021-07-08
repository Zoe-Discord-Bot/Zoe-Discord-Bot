package ch.kalunight.zoe.command.clash;

import java.sql.SQLException;
import java.util.List;
import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreatePlayerCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.service.clashchannel.LoadClashTeamAndStartBanAnalyseWorker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class ClashAnalyseCommandRunnable {
  
  public static final String USAGE_NAME = "analysis";
  
  private ClashAnalyseCommandRunnable() {
    // hide default private constructor
  }
  
  public static void executeCommand(Server server, TextChannel channel, String args, Message loadingMessage, InteractionHook hook) throws SQLException {
    
    ClashChannel channelToTreat = null;
    List<ClashChannel> clashchannls = ClashChannelRepository.getClashChannels(server.serv_guildId);
    for(ClashChannel channelToCheck : clashchannls) {
      if(channelToCheck.clashChannel_channelId == channel.getIdLong()) {
        channelToTreat = channelToCheck;
        break;
      }
    }
    
    List<String> listArgs = CreatePlayerCommandRunnable.getParameterInParenteses(args);
    String regionName;
    String summonerName;
    
    if(channelToTreat == null) {
      if(listArgs.size() == 2) {
        regionName = listArgs.get(0);
        summonerName = listArgs.get(1);
      }else {
        CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "clashAnalyzeMalformedFormat"), loadingMessage, hook);
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
        CommandUtil.sendMessageWithClassicOrSlashCommand(String.format(LanguageManager.getText(server.getLanguage(), "clashAnalyzeMalformedFormatInsideClashChannel"), 
            channelToTreat.clashChannel_data.getSelectedPlatform().getName().toUpperCase()), loadingMessage, hook);
        return;
      }
      
    }
    
    
    Platform platorm = CreatePlayerCommandRunnable.getPlatform(regionName);
    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByNameWithRateLimit(platorm, summonerName);
    }catch(RiotApiException e) {
      RiotApiUtil.handleRiotApi(loadingMessage, e, server.getLanguage());
      return;
    }
    
    if(summoner != null) {
      LoadClashTeamAndStartBanAnalyseWorker loadClashTeamWorker = new LoadClashTeamAndStartBanAnalyseWorker(server, summoner.getId(), platorm, channel, channelToTreat);
      CommandUtil.sendMessageWithClassicOrSlashCommand(("*" + LanguageManager.getText(server.getLanguage(), "clashAnalyzeLoadStarted") + "*"), loadingMessage, hook);
      ServerThreadsManager.getClashChannelExecutor().execute(loadClashTeamWorker);
    }
    
  }
}
