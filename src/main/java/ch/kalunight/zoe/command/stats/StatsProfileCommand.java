package ch.kalunight.zoe.command.stats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.Champion;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;

public class StatsProfileCommand extends Command {

  private static final int NUMBER_OF_CHAMPIONS_IN_GRAPH = 6;
  private static final Map<Double, Object> MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS = new HashMap<>();
  private static final Map<Double, Object> MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS = new HashMap<>();
  private static final Map<Double, Object> MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS = new HashMap<>();
  private static final byte[] valueError = {};
  
  private static final Logger logger = LoggerFactory.getLogger(StatsProfileCommand.class);

  static {
    MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS.put(20000.0, "20K");
    MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS.put(40000.0, "40K");
    MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS.put(60000.0, "60K");
    MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS.put(80000.0, "80K");
    MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS.put(100000.0, "100K");
    
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(50000.0, "50K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(100000.0, "100K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(200000.0, "200K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(300000.0, "300K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(400000.0, "400K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(500000.0, "500K");

    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(500000.0, "500K");
    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(1000000.0, "1M");
    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(1500000.0, "1.5M");
    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(2000000.0, "2M");
    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(2500000.0, "2.5M");
    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(3000000.0, "3M");
  }
  
  public StatsProfileCommand() {
    this.name = "profile";
    this.arguments = "@playerMention";
    this.help = "Get information about the mentioned player.";
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);

    List<User> userList = event.getMessage().getMentionedUsers();
    if(userList.size() != 1) {
      event.reply("Please mention 1 player.");
      return;
    }

    User user = userList.get(0);

    Server server = ServerData.getServers().get(event.getGuild().getId());
    Player player = server.getPlayerByDiscordId(user.getId());

    if(player == null) {
      event.reply("The poeple mentioned is not a registered player !");
      return;
    }

    List<ChampionMastery> championsMasteries;
    try {
      championsMasteries = Zoe.getRiotApi().getChampionMasteriesBySummoner(player.getRegion(), player.getSummoner().getId());
    } catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        event.reply("Please retry, I got a minor internal error. Sorry about that :/");
        return;
      }
      logger.warn("Got a unexpected error : ", e);
      event.reply("I got a unexpected error, please retry.");
      return;
    }
    
    byte[] imageBytes = generateMasteriesChart(event, championsMasteries);
    
    MessageBuilderRequest.createProfileMessage(imageBytes, player, championsMasteries);
    
    event.getTextChannel().sendFile(imageBytes, event.getAuthor().getName() + "ChampionGraph.png",
        new MessageBuilder("Here your masteries (Test)").build()).queue();
  }

  private byte[] generateMasteriesChart(CommandEvent event, List<ChampionMastery> championsMasteries) {
    List<ChampionMastery> listHeigherChampion = getBestMasteries(championsMasteries, NUMBER_OF_CHAMPIONS_IN_GRAPH);
    CategoryChartBuilder masteriesGraphBuilder = new CategoryChartBuilder();
    
    masteriesGraphBuilder.chartTheme = ChartTheme.GGPlot2;
    masteriesGraphBuilder.title("Best Champions by Masteries of " + event.getAuthor().getName());
    
    CategoryChart masteriesGraph = masteriesGraphBuilder.build();
    masteriesGraph.getStyler().setAntiAlias(true);
    masteriesGraph.getStyler().setLegendVisible(false);
    
    if(getMoyenneMasteries(listHeigherChampion) < 50000) {
      masteriesGraph.setYAxisLabelOverrideMap(MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS);
      
    } else if(getMoyenneMasteries(listHeigherChampion) < 200000) {
      masteriesGraph.setYAxisLabelOverrideMap(MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS);
      
    }else {
      masteriesGraph.setYAxisLabelOverrideMap(MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS);
    }
    
    masteriesGraph.setXAxisTitle("Best Champions by Masteries");
    masteriesGraph.setYAxisTitle("Masteries points");
    
    List<Double> xPointsMasteries = new ArrayList<>();
    List<Object> yName = new ArrayList<>();
    
    for(int i = 0; i < listHeigherChampion.size(); i++) {
      Champion actualSeriesChampion = Ressources.getChampionDataById(listHeigherChampion.get(i).getChampionId());
      
      String championName = "Champion Unknown";
      if(actualSeriesChampion != null) {
        championName = actualSeriesChampion.getName();
      }
      
      xPointsMasteries.add((double) listHeigherChampion.get(i).getChampionPoints());
      yName.add(championName);
    }
    
    masteriesGraph.addSeries("Champions", yName, xPointsMasteries);
    
    byte[] imageBytes;
    try {
      imageBytes = BitmapEncoder.getBitmapBytes(masteriesGraph, BitmapFormat.PNG);
    } catch (IOException e) {
      logger.warn("Got a error in encoding bytesMap image", e);
      event.reply("I got an unexpected error when i creating the graph, please retry.");
      return valueError;
    }
    return imageBytes;
  }

  private long getMoyenneMasteries(List<ChampionMastery> championsMasteries) {
    long allMasteries = 0;
    for(ChampionMastery championMastery : championsMasteries) {
      allMasteries += championMastery.getChampionPoints();
    }
    return allMasteries / championsMasteries.size();
  }

  private List<ChampionMastery> getBestMasteries(List<ChampionMastery> championsMasteries, int nbrTop) {
    List<ChampionMastery> listHeigherChampion = new ArrayList<>();
    
    for(int i = 0; i < nbrTop; i++) {

      ChampionMastery heigherActual = null;

      for(ChampionMastery championMastery : championsMasteries) {
        
        if(listHeigherChampion.contains(championMastery)) {
          continue;
        }
        
        if(heigherActual == null) {
          heigherActual = championMastery;
          continue;
        }

        if(championMastery.getChampionPoints() > heigherActual.getChampionPoints() && !listHeigherChampion.contains(heigherActual)) {
          heigherActual = championMastery;
        }
      }

      listHeigherChampion.add(heigherActual);
    }
    return listHeigherChampion;
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Stats profile command :\n");
        stringBuilder.append("--> `>delete " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }

}
