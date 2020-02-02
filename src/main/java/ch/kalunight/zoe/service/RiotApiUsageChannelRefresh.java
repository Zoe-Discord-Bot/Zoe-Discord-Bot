package ch.kalunight.zoe.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.style.PieStyler;
import org.knowm.xchart.style.PieStyler.AnnotationType;
import org.knowm.xchart.style.Styler.ChartTheme;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.riotapi.CachedRiotApi;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.rithms.riot.constant.Platform;

public class RiotApiUsageChannelRefresh implements Runnable {

  private static final int TIME_BETWEEN_EACH_RESET_CATCHED_RIOT_API_IN_DAY = 3;

  private static DateTime lastRapiCountReset = DateTime.now();

  private static Integer infocardCreatedCount = 0;

  private static TextChannel rapiInfoChannel;
  
  @Override
  public void run() {
    if(rapiInfoChannel != null) {

      cleanChannel();

      rapiInfoChannel.sendMessage("**Generic Stats**"
          + "\nTotal number of Servers : " + Zoe.getJda().getGuilds().size()
          + "\nTask in Server Executor Queue : " + ServerData.getServerExecutor().getQueue().size()
          + "\nInfoPannel refresh done last two minutes : " + InfoPanelRefresher.getNbrServerSefreshedLast2Minutes()
          + "\nTask in InfoCards Generator Queue : " + ServerData.getInfocardsGenerator().getQueue().size()
          + "\nTask in Players Data Worker Queue : " + ServerData.getPlayersDataQueue()
          + "\nInfocards Generated last 2 minutes : " + getInfocardCreatedCount()).queue();

      InfoPanelRefresher.getNbrServerSefreshedLast2Minutes().set(0);
      
      rapiInfoChannel.sendMessage("**Riot Request Stats**"
          + "\nTotal of requests with Riot api : " + Zoe.getRiotApi().getTotalRequestCount()
          + "\nNumber of request for match with RiotAPI : " + Zoe.getRiotApi().getApiMatchRequestCount()
          + "\nTotal number of request for match : " + Zoe.getRiotApi().getAllMatchRequestCount()).queue();
      
      rapiInfoChannel.sendMessage("**Discord Command Stats**"
          + "\nTotal discord command executed : " + ZoeCommand.getCommandExecuted().get() 
          + "\nTotal discord command done correctly : " + ZoeCommand.getCommandFinishedCorrectly().get()
          + "\nTotal discord command done with error : " + ZoeCommand.getCommandFinishedWithError().get()).queue();

      if(DateTime.now().minusDays(TIME_BETWEEN_EACH_RESET_CATCHED_RIOT_API_IN_DAY).isAfter(lastRapiCountReset)) {
        lastRapiCountReset = DateTime.now();
        ZoeCommand.clearStats();
      }

      setInfocardCreatedCount(0);

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

      for(int i = 0; i < graphs.size(); i++) {
        rapiInfoChannel.sendMessage(descriptions.get(i)).addFile(graphs.get(i), "graphFor" + platformOrder.get(i).getName() + ".png").queue();
      }
      
      if(Zoe.getRiotApi().isApiCallPerPlatformNeedToBeReset()) {
        Zoe.getRiotApi().resetApiCallPerPlatform();
      }
    }
  }

  private void cleanChannel() {
    List<Message> messagesToDelete = rapiInfoChannel.getIterableHistory().stream()
        .collect(Collectors.toList());

    try {
      rapiInfoChannel.purgeMessages(messagesToDelete);
    }catch(InsufficientPermissionException e) {
      rapiInfoChannel.sendMessage("I cannot clean all the channel, please give me the right to delete messages of all peoples").queue();
      List<Message> onlyMyMessagesToDelete = rapiInfoChannel.getIterableHistory().stream()
          .filter(m -> m.getAuthor().equals(Zoe.getJda().getSelfUser()))
          .collect(Collectors.toList());
      rapiInfoChannel.purgeMessages(onlyMyMessagesToDelete);
    }
  }

  public static TextChannel getRapiInfoChannel() {
    return rapiInfoChannel;
  }

  public static void setRapiInfoChannel(TextChannel rapiInfoChannel) {
    RiotApiUsageChannelRefresh.rapiInfoChannel = rapiInfoChannel;
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

}
