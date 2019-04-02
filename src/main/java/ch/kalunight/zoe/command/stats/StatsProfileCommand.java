package ch.kalunight.zoe.command.stats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
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
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;

public class StatsProfileCommand extends Command {

  private static final int NUMBER_OF_CHAMPIONS_IN_GRAPH = 6;
  private static final Logger logger = LoggerFactory.getLogger(StatsProfileCommand.class);

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

    List<ChampionMastery> listHeigherChampion = new ArrayList<>();

    for(int i = 0; i < NUMBER_OF_CHAMPIONS_IN_GRAPH; i++) {

      ChampionMastery heigherActual = null;

      for(ChampionMastery championMastery : championsMasteries) {
        if(heigherActual == null) {
          heigherActual = championMastery;
          continue;
        }

        if(championMastery.getChampionPoints() > heigherActual.getChampionPoints()) {
          heigherActual = championMastery;
        }
      }

      listHeigherChampion.add(heigherActual);
      championsMasteries.remove(heigherActual);
    }
    
    CategoryChartBuilder masteriesGraphBuilder = new CategoryChartBuilder();
    
    masteriesGraphBuilder.chartTheme = ChartTheme.XChart;
    masteriesGraphBuilder.title("Test : Best Champion Masteries of " + event.getAuthor().getName());
    CategoryChart masteriesGraph = masteriesGraphBuilder.build();
    
    Map<Double, Object> mappingXChart = new HashMap<>();
    
    double inc = 0;
    for(ChampionMastery championMastery : listHeigherChampion) {
      Champion actualSeriesChampion = Ressources.getChampionDataById(championMastery.getChampionId());
      
      String championName = "Champion Unknown";
      if(actualSeriesChampion != null) {
        championName = actualSeriesChampion.getName();
      }
      
      double[] points = {championMastery.getChampionPoints()};
      double[] valueChartMapping = {inc};
      mappingXChart.put(Double.valueOf(inc), championName);
      masteriesGraph.addSeries(championName, valueChartMapping, points);
      inc++;
    }
    
    masteriesGraph.setXAxisLabelOverrideMap(mappingXChart);
    
    byte[] imageBytes;
    try {
      imageBytes = BitmapEncoder.getBitmapBytes(masteriesGraph, BitmapFormat.PNG);
    } catch (IOException e) {
      logger.warn("Got a error in encoding bytesMap image", e);
      event.reply("I got an unexpected error when i creating the graph, please retry.");
      return;
    }
    event.getTextChannel().sendFile(imageBytes, event.getAuthor().getName() + "ChampionGraph.png",
        new MessageBuilder("Here your masteries (Test)").build()).queue();
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
