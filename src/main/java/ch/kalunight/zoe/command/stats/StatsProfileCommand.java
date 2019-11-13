package ch.kalunight.zoe.command.stats;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;
import net.rithms.riot.constant.CallPriority;

public class StatsProfileCommand extends ZoeCommand {

  private static final int NUMBER_OF_CHAMPIONS_IN_GRAPH = 6;
  private static final Map<Double, Object> MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS = new HashMap<>();
  private static final Map<Double, Object> MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS = new HashMap<>();
  private static final Map<Double, Object> MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS = new HashMap<>();

  private static final Logger logger = LoggerFactory.getLogger(StatsProfileCommand.class);
  
  private final EventWaiter waiter;

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
  

  public StatsProfileCommand(EventWaiter eventWaiter) {
    this.name = "profile";
    String[] aliases = {"player", "players", "p"};
    this.aliases = aliases;
    this.arguments = "@playerMention";
    this.help = "statsProfileHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(StatsCommand.USAGE_NAME, name, arguments, help);
    this.waiter = eventWaiter;
    Permission[] botPermissionNeeded = {Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_MANAGE};
    this.botPermissions = botPermissionNeeded;
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    
    Server server = ServerData.getServers().get(event.getGuild().getId());
    SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
        .setEventWaiter(waiter)
        .useLooping(true)
        .setColor(Color.GREEN)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server))
        .setTimeout(1, TimeUnit.MINUTES);

    List<User> userList = event.getMessage().getMentionedUsers();
    if(userList.size() != 1) {
      
      event.reply(LanguageManager.getText(server.getLangage(), "statsProfileMentionOnePlayer"));
      return;
    }

    User user = userList.get(0);
    Player player = server.getPlayerByDiscordId(user.getId());

    if(player == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "statsProfileNeedARegisteredPlayer"));
      return;
    }

    if(player.getLolAccounts().size() == 1) {
      generateStatsMessage(event, player, player.getLolAccounts().get(0), server);
    }else {
      selectAccountBuilder
      .addUsers(event.getAuthor())
      .setSelectionConsumer(getSelectionDoneAction(event, player, server));

      List<String> accountsName = new ArrayList<>();

      for(LeagueAccount choiceAccount : player.getLolAccounts()) {
        selectAccountBuilder.addChoices(String.format(LanguageManager.getText(server.getLangage(), "showPlayerAccount"),
            RiotRequest.getSoloqRank(choiceAccount.getSummoner().getId(), choiceAccount.getRegion(), CallPriority.HIGH),
            choiceAccount.getRegion(), CallPriority.HIGH));
        accountsName.add(choiceAccount.getSummoner().getName());
      }
      
      selectAccountBuilder.setText(getUpdateMessageAfterChangeSelectAction(accountsName, server));
      
      SelectionDialog selectAccount = selectAccountBuilder.build();
      selectAccount.display(event.getChannel());
    }
  }

  private Function<Integer, String> getUpdateMessageAfterChangeSelectAction(List<String> choices, Server server) {
    return new Function<Integer, String>() {
      @Override
      public String apply(Integer index) {
        return String.format(LanguageManager.getText(server.getLangage(), "statsProfileSelectText"), choices.get(index - 1));
      }
    };
  }

  private BiConsumer<Message, Integer> getSelectionDoneAction(CommandEvent event, Player player, Server server) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfUser) {
        LeagueAccount account = player.getLolAccounts().get(selectionOfUser - 1);

        selectionMessage.clearReactions().queue();
        
        selectionMessage.getTextChannel().sendMessage(String.format(
            LanguageManager.getText(server.getLangage(), "statsProfileSelectionDoneMessage"),
            account.getSummoner().getName(), player.getDiscordUser().getName())).queue();
        
        generateStatsMessage(event, player, player.getLolAccounts().get(selectionOfUser - 1), server);
      }

    };
  }

  private Consumer<Message> getSelectionCancelAction(Server server){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
        message.editMessage(LanguageManager.getText(server.getLangage(), "statsProfileSelectionEnded")).queue();
      }
    };
  }

  private void generateStatsMessage(CommandEvent event, Player player, LeagueAccount lolAccount, Server server) {
    event.getTextChannel().sendTyping().queue();
    
    List<ChampionMastery> championsMasteries;
    try {
      championsMasteries = Zoe.getRiotApi().getChampionMasteriesBySummoner(lolAccount.getRegion(), lolAccount.getSummoner().getId(), CallPriority.HIGH);
    } catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        event.reply(LanguageManager.getText(server.getLangage(), "statsProfileRateLimitError"));
        return;
      }
      logger.warn("Got a unexpected error : ", e);
      event.reply(LanguageManager.getText(server.getLangage(), "statsProfileUnexpectedError"));
      return;
    }

    byte[] imageBytes;
    try {
      imageBytes = generateMasteriesChart(player, championsMasteries, server);
    } catch(IOException e) {
      logger.info("Got a error in encoding bytesMap image : {}", e);
      event.reply(LanguageManager.getText(server.getLangage(), "statsProfileUnexpectedErrorGraph"));
      return;
    }

    MessageEmbed embed;
    try {
      embed = MessageBuilderRequest.createProfileMessage(player, lolAccount, championsMasteries);
    } catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        logger.debug("Get rate limited : {}", e);
        event.reply(LanguageManager.getText(server.getLangage(), "statsProfileRateLimitError"));
        return;
      }
      logger.warn("Got a unexpected error : {}", e);
      event.reply(LanguageManager.getText(server.getLangage(), "statsProfileUnexpectedError"));
      return;
    }

    MessageBuilder messageBuilder = new MessageBuilder();

    messageBuilder.setEmbed(embed);

    event.getTextChannel().sendMessage(messageBuilder.build()).addFile(imageBytes, player.getDiscordUser().getId() + ".png").queue();
  }

  private byte[] generateMasteriesChart(Player player, List<ChampionMastery> championsMasteries, Server server) throws IOException {
    List<ChampionMastery> listHeigherChampion = getBestMasteries(championsMasteries, NUMBER_OF_CHAMPIONS_IN_GRAPH);
    CategoryChartBuilder masteriesGraphBuilder = new CategoryChartBuilder();

    masteriesGraphBuilder.chartTheme = ChartTheme.GGPlot2;

    masteriesGraphBuilder.title(String.format(LanguageManager.getText(server.getLangage(), "statsProfileGraphTitle"),
        player.getDiscordUser().getName()));

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

    masteriesGraph.setXAxisTitle(LanguageManager.getText(server.getLangage(), "statsProfileGraphTitleX"));
    masteriesGraph.setYAxisTitle(LanguageManager.getText(server.getLangage(), "statsProfileGraphTitleY"));

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

    return BitmapEncoder.getBitmapBytes(masteriesGraph, BitmapFormat.PNG);
  }

  private long getMoyenneMasteries(List<ChampionMastery> championsMasteries) {
    long allMasteries = 0;
    for(ChampionMastery championMastery : championsMasteries) {
      allMasteries += championMastery.getChampionPoints();
    }
    return allMasteries / championsMasteries.size();
  }

  public static List<ChampionMastery> getBestMasteries(List<ChampionMastery> championsMasteries, int nbrTop) {
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
}
