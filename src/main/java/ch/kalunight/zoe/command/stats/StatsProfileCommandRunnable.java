package ch.kalunight.zoe.command.stats;

import java.awt.Color;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreatePlayerCommandRunnable;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.RiotApiUtil;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import no.stelar7.api.r4j.basic.exceptions.APIHTTPErrorReason;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;

public class StatsProfileCommandRunnable {

  private static final int NUMBER_OF_CHAMPIONS_IN_GRAPH = 6;
  private static final Map<Double, Object> MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS = new HashMap<>();
  private static final Map<Double, Object> MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS = new HashMap<>();
  private static final Map<Double, Object> MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS = new HashMap<>();

  private static final Logger logger = LoggerFactory.getLogger(StatsProfileCommandRunnable.class);

  private static final Random random = new Random();

  static {
    MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS.put(20000.0, "20K");
    MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS.put(40000.0, "40K");
    MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS.put(60000.0, "60K");
    MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS.put(80000.0, "80K");
    MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS.put(100000.0, "100K");

    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(50000.0, "50K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(100000.0, "100K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(150000.0, "150K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(200000.0, "200K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(250000.0, "250K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(300000.0, "300K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(350000.0, "350K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(400000.0, "400K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(450000.0, "450K");
    MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS.put(500000.0, "500K");

    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(500000.0, "500K");
    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(1000000.0, "1M");
    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(1500000.0, "1.5M");
    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(2000000.0, "2M");
    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(2500000.0, "2.5M");
    MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS.put(3000000.0, "3M");
  }
  
  private StatsProfileCommandRunnable() {
    // hide default public constructor
  }
  
  public static void executeCommand(Server server, TextChannel sourceChannel, String args, List<User> mentionnedMembers,
      Message toEdit, InteractionHook hook, EventWaiter waiter, Member author, boolean forceRefresh) throws SQLException {
    
    DTO.LeagueAccount leagueAccount = getLeagueAccountWithParam(args, server, sourceChannel.getJDA());

    if(leagueAccount != null) {
      try {
        leagueAccount.getSummoner(forceRefresh);
      } catch(APIResponseException e) {
        CommandUtil.sendMessageWithClassicOrSlashCommand(RiotApiUtil.getTextHandlerRiotApiError(e, server.getLanguage()), toEdit, hook);
        return;
      }

      generateStatsMessage(null, leagueAccount, server, toEdit, hook, sourceChannel, forceRefresh);
      return;
    }

    List<User> userList = mentionnedMembers;
    if(userList.size() != 1) {
      CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "statsProfileMentionOnePlayer"), toEdit, hook);
      return;
    }

    User user = userList.get(0);
    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
    if(player == null) {
      CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "statsProfileNeedARegisteredPlayer"), toEdit, hook);
      return;
    }

    List<DTO.LeagueAccount> accounts = LeagueAccountRepository.getLeaguesAccounts(server.serv_guildId, user.getIdLong());
    
    SavedSummoner summoner;
    if(accounts.size() == 1) {
      generateStatsMessage(player, accounts.get(0), server, toEdit, hook, sourceChannel, forceRefresh);
    }else if(accounts.isEmpty()) {
      CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "statsProfileNeedARegisteredAccount"), toEdit, hook);
    }else {

      SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
          .setEventWaiter(waiter)
          .useLooping(true)
          .setColor(Color.GREEN)
          .setSelectedEnds("**", "**")
          .setCanceled(getSelectionCancelAction(server))
          .setTimeout(1, TimeUnit.MINUTES);

      selectAccountBuilder
      .addUsers(author.getUser())
      .setSelectionConsumer(getSelectionDoneAction(player, server, accounts, toEdit, hook, sourceChannel, forceRefresh));

      List<String> accountsName = new ArrayList<>();

      for(DTO.LeagueAccount choiceAccount : accounts) {
        try {
          summoner = Zoe.getRiotApi().getSummonerBySummonerId(choiceAccount.leagueAccount_server, choiceAccount.leagueAccount_summonerId, forceRefresh);
        }catch(APIResponseException e) {
          CommandUtil.sendMessageWithClassicOrSlashCommand(RiotApiUtil.getTextHandlerRiotApiError(e, server.getLanguage()), toEdit, hook);
          return;
        }

        selectAccountBuilder.addChoices(String.format(LanguageManager.getText(server.getLanguage(), "showPlayerAccount"),
            summoner.getName(),
            choiceAccount.leagueAccount_server.getShowableName(),
            RiotRequest.getSoloqRank(choiceAccount.leagueAccount_summonerId, choiceAccount.leagueAccount_server).toString(server.getLanguage())));
        accountsName.add(summoner.getName());
      }

      selectAccountBuilder.setText(getUpdateMessageAfterChangeSelectAction(accountsName, server));

      SelectionDialog selectAccount = selectAccountBuilder.build();
      selectAccount.display(sourceChannel);
    }
  }

  private static LeagueAccount getLeagueAccountWithParam(String args, Server server, JDA jda) {

    List<String> listArgs = CreatePlayerCommandRunnable.getParameterInParenteses(args);
    String regionName = "";
    String summonerName;
    if(listArgs.size() == 2) {
      regionName = listArgs.get(0);
      summonerName = listArgs.get(1);
    }else if(listArgs.size() == 1) {
      ServerConfiguration config;
      try {
        config = ConfigRepository.getServerConfiguration(server.serv_guildId, jda);
      } catch(SQLException e1) {
        return null;
      }
      RegionOption regionOption = config.getDefaultRegion();

      if(regionOption.getRegion() != null) {
        regionName = regionOption.getRegion().getShowableName();
      }
      summonerName = listArgs.get(0);
    }else {
      return null;
    }

    ZoePlatform region = CreatePlayerCommandRunnable.getPlatform(regionName);
    if(region == null) {
      return null;
    }

    SavedSummoner summoner;
    SavedSummoner tftSummoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
      tftSummoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
    }catch(APIResponseException e) {
      return null;
    }
    return new LeagueAccount(summoner, tftSummoner, region);
  }

  private static Function<Integer, String> getUpdateMessageAfterChangeSelectAction(List<String> choices, DTO.Server server) {
    return new Function<Integer, String>() {
      @Override
      public String apply(Integer index) {
        return String.format(LanguageManager.getText(server.getLanguage(), "statsProfileSelectText"), choices.get(index - 1));
      }
    };
  }

  private static BiConsumer<Message, Integer> getSelectionDoneAction(DTO.Player player,
      DTO.Server server, List<DTO.LeagueAccount> lolAccounts, Message messageLoading, InteractionHook hook, TextChannel channel, boolean forceRefresh) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfUser) {
        DTO.LeagueAccount account = lolAccounts.get(selectionOfUser - 1);

        selectionMessage.clearReactions().queue();

        try {
          selectionMessage.getTextChannel().sendMessage(String.format(
              LanguageManager.getText(server.getLanguage(), "statsProfileSelectionDoneMessage"),
              account.getSummoner(forceRefresh).getName(), channel.getJDA().retrieveUserById(player.player_discordId).complete()
              .getName())).queue();
        } catch (APIResponseException e) {
          CommandUtil.sendMessageWithClassicOrSlashCommand(RiotApiUtil.getTextHandlerRiotApiError(e, server.getLanguage()), messageLoading, hook);
          return;
        }

        generateStatsMessage(player, account, server, messageLoading, hook, channel, forceRefresh);
      }

    };
  }

  private static Consumer<Message> getSelectionCancelAction(DTO.Server server){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
        message.editMessage(LanguageManager.getText(server.getLanguage(), "statsProfileSelectionEnded")).queue();
      }
    };
  }

  private static void generateStatsMessage(DTO.Player player, DTO.LeagueAccount lolAccount, DTO.Server server, Message messageLoading,
      InteractionHook hook, TextChannel channel, boolean forceRefresh) {

    String url = Integer.toString(random.nextInt(100000));

    List<SavedSimpleMastery> championsMasteries;
    try {
      championsMasteries = Zoe.getRiotApi().getChampionMasteryBySummonerId(lolAccount.leagueAccount_server, lolAccount.leagueAccount_summonerId, forceRefresh);
    } catch(APIResponseException e) {
      if(e.getReason() == APIHTTPErrorReason.ERROR_429) {
        CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "statsProfileRateLimitError"), messageLoading, hook);
        return;
      }
      logger.warn("Got a unexpected error : ", e);
      CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "statsProfileUnexpectedError"), messageLoading, hook);
      return;
    }

    byte[] imageBytes = null;
    try {
      if(championsMasteries != null && !championsMasteries.isEmpty()) {
        imageBytes = generateMasteriesChart(player, championsMasteries, server, lolAccount, channel.getJDA(), forceRefresh);
      }
    } catch(IOException | APIResponseException e) {
      logger.info("Got a error in encoding bytesMap image", e);
      CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "statsProfileUnexpectedErrorGraph"), messageLoading, hook);
      return;
    }

    MessageEmbed embed;
    try {
      embed = MessageBuilderRequest.createProfileMessage(player, lolAccount, championsMasteries, server.getLanguage(), url, channel.getJDA(), forceRefresh);
    } catch(APIResponseException e) {
      if(e.getReason() == APIHTTPErrorReason.ERROR_429) {
        logger.debug("Get rate limited", e);
        CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "statsProfileRateLimitError"), messageLoading, hook);
        return;
      }
      logger.warn("Got a unexpected error");
      CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "statsProfileUnexpectedError"), messageLoading, hook);
      return;
    }

    MessageBuilder messageBuilder = new MessageBuilder();

    messageBuilder.setEmbeds(embed);

    if(imageBytes != null) {
      if(player != null) {
        channel.sendMessage(messageBuilder.build()).addFile(imageBytes, player.retrieveUser(channel.getJDA()).getId() + ".png").queue();
      }else {
        channel.sendMessage(messageBuilder.build()).addFile(imageBytes, url + ".png").queue();
      }
    }else {
      channel.sendMessage(messageBuilder.build()).queue();
    }
  }

  private static byte[] generateMasteriesChart(DTO.Player player, List<SavedSimpleMastery> championsMasteries,
      DTO.Server server, LeagueAccount leagueAccount, JDA jda, boolean forceRefresh) throws IOException {
    List<SavedSimpleMastery> listHeigherChampion = getBestMasteries(championsMasteries, NUMBER_OF_CHAMPIONS_IN_GRAPH);
    CategoryChartBuilder masteriesGraphBuilder = new CategoryChartBuilder();

    masteriesGraphBuilder.chartTheme = ChartTheme.GGPlot2;

    if(player != null) {
      masteriesGraphBuilder.title(String.format(LanguageManager.getText(server.getLanguage(), "statsProfileGraphTitle"),
          player.retrieveUser(jda).getName()));
    }else {
      masteriesGraphBuilder.title(String.format(LanguageManager.getText(server.getLanguage(), "statsProfileGraphTitle"),
          leagueAccount.getSummoner(forceRefresh).getName()));
    }

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

    masteriesGraph.setXAxisTitle(LanguageManager.getText(server.getLanguage(), "statsProfileGraphTitleX"));
    masteriesGraph.setYAxisTitle(LanguageManager.getText(server.getLanguage(), "statsProfileGraphTitleY"));

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

  private static long getMoyenneMasteries(List<SavedSimpleMastery> championsMasteries) {
    long allMasteries = 0;
    for(SavedSimpleMastery championMastery : championsMasteries) {
      if(championMastery != null) {
        allMasteries += championMastery.getChampionPoints();
      }
    }
    if(championsMasteries.isEmpty()) {
      return 0;
    }
    return allMasteries / championsMasteries.size();
  }

  public static List<SavedSimpleMastery> getBestMasteries(List<SavedSimpleMastery> championsMasteries, int nbrTop) {
    List<SavedSimpleMastery> listHeigherChampion = new ArrayList<>();

    for(int i = 0; i < nbrTop; i++) {

      SavedSimpleMastery heigherActual = null;

      for(SavedSimpleMastery championMastery : championsMasteries) {

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

      if(heigherActual != null) {
        listHeigherChampion.add(heigherActual);
      }
    }

    return listHeigherChampion;
  }
}
