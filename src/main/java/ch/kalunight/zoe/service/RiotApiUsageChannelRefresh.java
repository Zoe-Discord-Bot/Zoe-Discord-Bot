package ch.kalunight.zoe.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.style.PieStyler;
import org.knowm.xchart.style.PieStyler.AnnotationType;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.repositories.LeaderboardRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.riotapi.CachedRiotApi;
import ch.kalunight.zoe.service.infochannel.InfoPanelRefresher;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.rithms.riot.constant.Platform;

public class RiotApiUsageChannelRefresh implements Runnable {

  private static final int TIME_BETWEEN_EACH_RESET_CATCHED_RIOT_API_IN_DAY = 3;

  private static final Logger logger = LoggerFactory.getLogger(RiotApiUsageChannelRefresh.class);

  private static DateTime lastRapiCountReset = DateTime.now();

  private static Integer infocardCreatedCount = 0;
  
  private static Integer infocardCanceledCount = 0;

  private static long guildId;

  private static long textChannelId;

  @Override
  public void run() {

    try {

      if(guildId == 0) {
        return;
      }

      Guild guild = Zoe.getGuildById(guildId);
      TextChannel rapiInfoChannel = guild.getJDA().getTextChannelById(textChannelId);

      if(rapiInfoChannel != null) {

        cleanChannel(rapiInfoChannel);

        rapiInfoChannel.sendMessage("**Generic Stats**"
            + "\nTotal number of Servers : " + Zoe.getNumberOfGuilds()
            + "\nTask in Server Executor Queue : " + (ServerThreadsManager.getServerExecutor().getActiveCount() + ServerThreadsManager.getServerExecutor().getQueue().size())
            + "\nInfoPannel refresh done last two minutes : " + InfoPanelRefresher.getNbrServerSefreshedLast2Minutes()
            + "\nTask in Players Data Worker Queue : " + ServerThreadsManager.getPlayersDataQueue()
            + "\nInfocards Generated last 2 minutes : " + getInfocardCreatedCount()
            + "\nInfocards Canceled last 2 minutes : " + getInfocardCanceledCount()
            + "\nTask in Leaderboard Executor : " + ServerThreadsManager.getLeaderboardExecutor().getQueue().size()
            + "\nTask in Clash Channel Executor : " + ServerThreadsManager.getClashChannelExecutor().getQueue().size()
            + "\nTask in Analysis Manager : " + ServerThreadsManager.getDataAnalysisManager().getQueue().size()
            + "\nTask in Analysis Thread : " + ServerThreadsManager.getDataAnalysisThread().getQueue().size()
            + "\nTask in Events Executor : " + ServerThreadsManager.getServerExecutor().getQueue().size()).queue();

        StringBuilder refreshStatusText = new StringBuilder();

        TreatServerService treatServerService = ServerChecker.getServerRefreshService();

        if(treatServerService != null) {
        refreshStatusText.append("**Refresh Status**"
            + "\nNumber Of Server Managed : " + treatServerService.getNumberOfServerManaged()
            + "\nServer Approximate Refresh Rate : " + treatServerService.getEstimateTimeToFullRefreshInMinutes()
            + "\nServer Refreshed Per Min : " + treatServerService.getServerRefreshedEachMinute()
            + "\n**Queue Health**"
            + "\nQueue Size Discord Status : " + treatServerService.getQueueSizeDiscordStatus()
            + "\nQueue Size Asked Refresh : " + treatServerService.getQueueSizeAskedToRefresh()
            + "\nQueue Size InfoGame Card : " + treatServerService.getQueueSizeInfoCardsToRefresh()
            + "\nQueue Size Passive refresh : " + treatServerService.getQueueSizePassiveRefresh()
            + "\nCycle started the at (UTC) : " + treatServerService.getCycleStart().toString());
        }
        
        rapiInfoChannel.sendMessage(refreshStatusText.toString()).queue();

        StringBuilder serverHelperStats = new StringBuilder();
        serverHelperStats.append("**Server Helper Threads Stats**\n");

        for(Platform platform : Platform.values()) {
          ThreadPoolExecutor threadsPool = ServerThreadsManager.getInfochannelHelperThread(platform);

          serverHelperStats.append(platform.getName().toUpperCase() + " queue : " + threadsPool.getQueue().size() + "\n");
        }

        rapiInfoChannel.sendMessage(serverHelperStats.toString()).queue();

        rapiInfoChannel.sendMessage("**Usage Stats**"
            + "\nTotal number of Players : " + PlayerRepository.countPlayers()
            + "\nTotal number of League Accounts : " + LeagueAccountRepository.countLeagueAccounts()
            + "\nTotal number of Leaderboards : " + LeaderboardRepository.countLeaderboards()).queue();

        InfoPanelRefresher.getNbrServerSefreshedLast2Minutes().set(0);

        rapiInfoChannel.sendMessage("**Riot Request Stats**"
            + "\nTotal of requests with Riot api : " + Zoe.getRiotApi().getTotalRequestCount()
            + "\nNumber of request for match with RiotAPI : " + Zoe.getRiotApi().getApiMatchRequestCount()
            + "\nNumber of request for match with DB : " + (Zoe.getRiotApi().getAllMatchRequestCount() - Zoe.getRiotApi().getApiMatchRequestCount())).queue();

        rapiInfoChannel.sendMessage("**Discord Command Stats**"
            + "\nTotal discord command executed : " + ZoeCommand.getCommandExecuted().get() 
            + "\nTotal discord command done correctly : " + ZoeCommand.getCommandFinishedCorrectly().get()
            + "\nTotal discord command done with error : " + ZoeCommand.getCommandFinishedWithError().get()
            + "\nTotal Slash command executed : " + ZoeSlashCommand.getCommandExecuted().get() 
            + "\nTotal Slash command done correctly : " + ZoeSlashCommand.getCommandFinishedCorrectly().get()
            + "\nTotal Slash command done with error : " + ZoeSlashCommand.getCommandFinishedWithError().get()).queue();
        
        if(DateTime.now().minusDays(TIME_BETWEEN_EACH_RESET_CATCHED_RIOT_API_IN_DAY).isAfter(lastRapiCountReset)) {
          lastRapiCountReset = DateTime.now();
          ZoeCommand.clearStats();
          ZoeSlashCommand.clearStats();
        }

        setInfocardCreatedCount(0);
        setInfocardCanceledCount(0);

        ArrayList<byte[]> graphs = new ArrayList<>();
        List<Platform> platformOrder = new ArrayList<>();
        List<Message> descriptions = new ArrayList<>();
        for(Platform platform : Platform.values()) {
          long numberOfRequestRemaining = Zoe.getRiotApi().getApiCallRemainingPerRegion(platform);

          PieChart pieChart = new PieChartBuilder()
              .title("Request data for " + platform.getName())
              .theme(ChartTheme.GGPlot2)
              .build();

          PieStyler styler = pieChart.getStyler();
          styler.setAntiAlias(true);
          styler.setAnnotationType(AnnotationType.LabelAndValue);
          styler.setAnnotationDistance(1.1);
          styler.setHasAnnotations(true);

          pieChart.addSeries("Calls Used", CachedRiotApi.RIOT_API_HUGE_LIMIT - numberOfRequestRemaining);
          pieChart.addSeries("Calls avaible", numberOfRequestRemaining);

          try {
            graphs.add(BitmapEncoder.getBitmapBytes(pieChart, BitmapFormat.PNG));
            platformOrder.add(platform);

            MessageBuilder description = new MessageBuilder();
            description.append("Status of Api for " + platform.getName() + ". Max Calls : " 
                + CachedRiotApi.RIOT_API_HUGE_LIMIT + " Calls Used : " 
                + (CachedRiotApi.RIOT_API_HUGE_LIMIT - numberOfRequestRemaining));

            descriptions.add(description.build());
          } catch(IOException e) {
            rapiInfoChannel.sendMessage("Got an error when generating graph for " + platform.getName()).queue();
          }
        }

        for(Platform platform : Platform.values()) {
          long numberOfRequestRemaining = Zoe.getRiotApi().getApiCallRemainingPerRegionTFT(platform);

          PieChart pieChart = new PieChartBuilder()
              .title("Request data for " + platform.getName() + " (TFT)")
              .theme(ChartTheme.GGPlot2)
              .build();

          PieStyler styler = pieChart.getStyler();
          styler.setAntiAlias(true);
          styler.setAnnotationType(AnnotationType.LabelAndValue);
          styler.setAnnotationDistance(1.1);
          styler.setHasAnnotations(true);

          pieChart.addSeries("Calls Used", CachedRiotApi.RIOT_API_TFT_HUGE_LIMIT - numberOfRequestRemaining);
          pieChart.addSeries("Calls avaible", numberOfRequestRemaining);

          try {
            graphs.add(BitmapEncoder.getBitmapBytes(pieChart, BitmapFormat.PNG));
            platformOrder.add(platform);

            MessageBuilder description = new MessageBuilder();
            description.append("Status of Api for " + platform.getName() + " (TFT). Max Calls : " 
                + CachedRiotApi.RIOT_API_TFT_HUGE_LIMIT + " Calls Used : " 
                + (CachedRiotApi.RIOT_API_TFT_HUGE_LIMIT - numberOfRequestRemaining));

            descriptions.add(description.build());
          } catch(IOException e) {
            rapiInfoChannel.sendMessage("Got an error when generating graph for " + platform.getName()).queue();
          }
        }

        for(int i = 0; i < graphs.size(); i++) {
          rapiInfoChannel.sendMessage(descriptions.get(i)).addFile(graphs.get(i), "graphFor" + platformOrder.get(i).getName() + ".png").queue();
        }

        if(Zoe.getRiotApi().isApiCallPerPlatformNeedToBeReset()) {
          Zoe.getRiotApi().resetApiCallPerPlatform();
        }
      }
    }catch(Exception e) {
      logger.warn("Error when refreshing riot api usage channel !", e);
    }
  }

  private void cleanChannel(TextChannel rapiInfoChannel) {
    List<Message> messagesToDelete = rapiInfoChannel.getIterableHistory().stream()
        .collect(Collectors.toList());

    try {
      rapiInfoChannel.purgeMessages(messagesToDelete);
    }catch(InsufficientPermissionException e) {
      rapiInfoChannel.sendMessage("I cannot clean all the channel, please give me the right to delete messages of all peoples").queue();
      List<Message> onlyMyMessagesToDelete = rapiInfoChannel.getIterableHistory().stream()
          .filter(m -> m.getAuthor().equals(rapiInfoChannel.getJDA().getSelfUser()))
          .collect(Collectors.toList());
      rapiInfoChannel.purgeMessages(onlyMyMessagesToDelete);
    }
  }

  public static TextChannel getRapiInfoChannel() {
    Guild guild = Zoe.getGuildById(guildId);

    if(guild != null) {
      return guild.getTextChannelById(textChannelId);
    }
    return null;
  }

  public static synchronized Integer getInfocardCreatedCount() {
    return infocardCreatedCount;
  }

  public static synchronized void setInfocardCreatedCount(Integer infocardCreatedCount) {
    RiotApiUsageChannelRefresh.infocardCreatedCount = infocardCreatedCount;
  }

  public static synchronized void incrementInfocardCount() {
    infocardCreatedCount++;
  }
  
  public static synchronized void incrementInfocardCancelCount() {
    infocardCanceledCount++;
  }

  public static long getTextChannelId() {
    return textChannelId;
  }

  public static Integer getInfocardCanceledCount() {
    return infocardCanceledCount;
  }

  public static void setInfocardCanceledCount(Integer infocardCanceledCount) {
    RiotApiUsageChannelRefresh.infocardCanceledCount = infocardCanceledCount;
  }

  public static void setTextChannelId(long textChannelId) {
    RiotApiUsageChannelRefresh.textChannelId = textChannelId;
  }

  public static long getGuildId() {
    return guildId;
  }

  public static void setGuildId(long guildId) {
    RiotApiUsageChannelRefresh.guildId = guildId;
  }

}
