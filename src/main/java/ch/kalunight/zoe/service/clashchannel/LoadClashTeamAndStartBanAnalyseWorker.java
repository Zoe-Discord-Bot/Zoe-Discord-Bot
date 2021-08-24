package ch.kalunight.zoe.service.clashchannel;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.clash.ClashTeamRegistration;
import ch.kalunight.zoe.model.clash.TeamPlayerAnalysisDataCollector;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.service.analysis.TeamBanAnalysisWorker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.ClashUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import ch.kalunight.zoe.util.TeamUtil;
import net.dv8tion.jda.api.entities.TextChannel;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPlayer;

public class LoadClashTeamAndStartBanAnalyseWorker implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(LoadClashTeamAndStartBanAnalyseWorker.class);

  private Server server;

  private String summonerId;

  private ZoePlatform platform;

  private ClashChannel clashChannel;

  private TextChannel channelWhereToSend;

  public LoadClashTeamAndStartBanAnalyseWorker(Server server, String summonerId, ZoePlatform platform, TextChannel channelWhereToSend, ClashChannel clashChannel) {
    this.server = server;
    this.summonerId = summonerId;
    this.platform = platform;
    this.clashChannel = clashChannel;
    this.channelWhereToSend = channelWhereToSend;
  }

  @Override
  public void run() {
    try {

      List<ClashPlayer> clashPlayerRegistrations = Zoe.getRiotApi().getClashPlayerBySummonerId(platform, summonerId);

      if(clashPlayerRegistrations.isEmpty()) {
        channelWhereToSend.sendMessage(LanguageManager.getText(server.getLanguage(), "clashAnalyzeLoadNotRegistered")).queue();
        return;
      }

      ClashTeamRegistration clashTeamRegistration = ClashUtil.getFirstRegistration(platform, clashPlayerRegistrations);

      if(clashTeamRegistration.getTeam().getPlayers().size() == 5) {

        List<TeamPlayerAnalysisDataCollector> playersData = TeamUtil.getTeamPlayersDataWithAnalysisDoneWithClashData(platform, clashTeamRegistration.getTeam().getPlayers());

        TeamBanAnalysisWorker banAnalysisWorker = new TeamBanAnalysisWorker(server, clashChannel, clashTeamRegistration, channelWhereToSend, playersData);

        ServerThreadsManager.getClashChannelExecutor().execute(banAnalysisWorker);

      }else {
        channelWhereToSend.sendMessage(LanguageManager.getText(server.getLanguage(), "clashAnalyzeLoadNot5Players")).queue();
      }

    } catch (APIResponseException e) {
      RiotApiUtil.handleRiotApi(channelWhereToSend, e, server.getLanguage());
    } catch (Exception e) {
      logger.error("Unexpected error while loading clash team.", e);
      channelWhereToSend.sendMessage(LanguageManager.getText(server.getLanguage(), "statsProfileUnexpectedError")).queue();
    }

  }

}
