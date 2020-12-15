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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.SavedChampionMastery;
import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
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
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class StatsProfileCommand extends ZoeCommand {

  private static final int NUMBER_OF_CHAMPIONS_IN_GRAPH = 6;
  private static final Map<Double, Object> MASTERIES_TABLE_OF_HIGH_VALUE_Y_AXIS = new HashMap<>();
  private static final Map<Double, Object> MASTERIES_TABLE_OF_CLASSIC_VALUE_Y_AXIS = new HashMap<>();
  private static final Map<Double, Object> MASTERIES_TABLE_OF_LOW_VALUE_Y_AXIS = new HashMap<>();

  private static final Logger logger = LoggerFactory.getLogger(StatsProfileCommand.class);

  private static final Random random = new Random();

  private final EventWaiter waiter;

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


  public StatsProfileCommand(EventWaiter eventWaiter) {
    this.name = "profile";
    String[] aliases = {"player", "players", "p"};
    this.aliases = aliases;
    this.arguments = "@playerMention OR (Region) (summonerName)";
    this.help = "statsProfileHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(StatsCommand.USAGE_NAME, name, arguments, help);
    this.waiter = eventWaiter;
    Permission[] botPermissionNeeded = {Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_MANAGE};
    this.botPermissions = botPermissionNeeded;
    this.guildOnly = true;
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {

    DTO.Server server = getServer(event.getGuild().getIdLong());

    DTO.LeagueAccount leagueAccount = getLeagueAccountWithParam(event, server);

    if(leagueAccount != null) {
      try {
        leagueAccount.getSummoner();
      } catch(RiotApiException e) {
        RiotApiUtil.handleRiotApi(event.getEvent(), e, server.serv_language);
        return;
      }

      generateStatsMessage(event, null, leagueAccount, server);
      return;
    }

    List<User> userList = event.getMessage().getMentionedUsers();
    if(userList.size() != 1) {

      event.reply(LanguageManager.getText(server.serv_language, "statsProfileMentionOnePlayer"));
      return;
    }

    User user = userList.get(0);
    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
    if(player == null) {
      event.reply(LanguageManager.getText(server.serv_language, "statsProfileNeedARegisteredPlayer"));
      return;
    }

    List<DTO.LeagueAccount> accounts = LeagueAccountRepository.getLeaguesAccounts(server.serv_guildId, user.getIdLong());

    SavedSummoner summoner;
    if(accounts.size() == 1) {
      try {
        summoner = Zoe.getRiotApi().getSummoner(accounts.get(0).leagueAccount_server,
            accounts.get(0).leagueAccount_summonerId, false);
      }catch(RiotApiException e) {
        RiotApiUtil.handleRiotApi(event.getEvent(), e, server.serv_language);
        return;
      }
      generateStatsMessage(event, player, accounts.get(0), server);
    }else if(accounts.isEmpty()) {
      event.reply(LanguageManager.getText(server.serv_language, "statsProfileNeedARegisteredAccount"));
    }else {

      SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
          .setEventWaiter(waiter)
          .useLooping(true)
          .setColor(Color.GREEN)
          .setSelectedEnds("**", "**")
          .setCanceled(getSelectionCancelAction(server))
          .setTimeout(1, TimeUnit.MINUTES);

      selectAccountBuilder
      .addUsers(event.getAuthor())
      .setSelectionConsumer(getSelectionDoneAction(event, player, server, accounts));

      List<String> accountsName = new ArrayList<>();

      for(DTO.LeagueAccount choiceAccount : accounts) {
        try {
          summoner = Zoe.getRiotApi().getSummoner(choiceAccount.leagueAccount_server,
              choiceAccount.leagueAccount_summonerId, false);
        }catch(RiotApiException e) {
          RiotApiUtil.handleRiotApi(event.getEvent(), e, server.serv_language);
          return;
        }

        selectAccountBuilder.addChoices(String.format(LanguageManager.getText(server.serv_language, "showPlayerAccount"),
            summoner.getName(),
            choiceAccount.leagueAccount_server.getName().toUpperCase(),
            RiotRequest.getSoloqRank(choiceAccount.leagueAccount_summonerId, choiceAccount.leagueAccount_server).toString(server.serv_language)));
        accountsName.add(summoner.getName());
      }

      selectAccountBuilder.setText(getUpdateMessageAfterChangeSelectAction(accountsName, server));

      SelectionDialog selectAccount = selectAccountBuilder.build();
      selectAccount.display(event.getChannel());
    }
  }

  private LeagueAccount getLeagueAccountWithParam(CommandEvent event, Server server) {

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    String regionName = "";
    String summonerName;
    if(listArgs.size() == 2) {
      regionName = listArgs.get(0);
      summonerName = listArgs.get(1);
    }else if(listArgs.size() == 1) {
      ServerConfiguration config;
      try {
        config = ConfigRepository.getServerConfiguration(server.serv_guildId);
      } catch(SQLException e1) {
        return null;
      }
      RegionOption regionOption = config.getDefaultRegion();

      if(regionOption.getRegion() != null) {
        regionName = regionOption.getRegion().getName();
      }
      summonerName = listArgs.get(0);
    }else {
      return null;
    }

    Platform region = CreatePlayerCommand.getPlatform(regionName);
    if(region == null) {
      return null;
    }

    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
    }catch(RiotApiException e) {
      return null;
    }
    return new LeagueAccount(summoner, region);
  }

  private Function<Integer, String> getUpdateMessageAfterChangeSelectAction(List<String> choices, DTO.Server server) {
    return new Function<Integer, String>() {
      @Override
      public String apply(Integer index) {
        return String.format(LanguageManager.getText(server.serv_language, "statsProfileSelectText"), choices.get(index - 1));
      }
    };
  }

  private BiConsumer<Message, Integer> getSelectionDoneAction(CommandEvent event, DTO.Player player,
      DTO.Server server, List<DTO.LeagueAccount> lolAccounts) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfUser) {
        DTO.LeagueAccount account = lolAccounts.get(selectionOfUser - 1);

        selectionMessage.clearReactions().queue();

        try {
          selectionMessage.getTextChannel().sendMessage(String.format(
              LanguageManager.getText(server.serv_language, "statsProfileSelectionDoneMessage"),
              account.getSummoner().getName(), Zoe.getJda().retrieveUserById(player.player_discordId).complete()
              .getName())).queue();
        } catch (RiotApiException e) {
          RiotApiUtil.handleRiotApi(event.getEvent(), e, server.serv_language);
          return;
        }

        generateStatsMessage(event, player, account, server);
      }

    };
  }

  private Consumer<Message> getSelectionCancelAction(DTO.Server server){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
        message.editMessage(LanguageManager.getText(server.serv_language, "statsProfileSelectionEnded")).queue();
      }
    };
  }

  private void generateStatsMessage(CommandEvent event, DTO.Player player, DTO.LeagueAccount lolAccount, DTO.Server server) {
    event.getTextChannel().sendTyping().queue();

    String url = Integer.toString(random.nextInt(100000));

    SavedChampionMastery championsMasteries;
    try {
      championsMasteries = Zoe.getRiotApi().getChampionMasteriesBySummoner(lolAccount.leagueAccount_server,
          lolAccount.leagueAccount_summonerId, true);
    } catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        event.reply(LanguageManager.getText(server.serv_language, "statsProfileRateLimitError"));
        return;
      }
      logger.warn("Got a unexpected error : ", e);
      event.reply(LanguageManager.getText(server.serv_language, "statsProfileUnexpectedError"));
      return;
    }

    byte[] imageBytes = null;
    try {
      if(championsMasteries != null && !championsMasteries.getChampionMasteries().isEmpty()) {
        imageBytes = generateMasteriesChart(player, championsMasteries, server, lolAccount);
      }
    } catch(IOException | RiotApiException e) {
      logger.info("Got a error in encoding bytesMap image : {}", e);
      event.reply(LanguageManager.getText(server.serv_language, "statsProfileUnexpectedErrorGraph"));
      return;
    }

    MessageEmbed embed;
    try {
      embed = MessageBuilderRequest.createProfileMessage(player, lolAccount, championsMasteries, server.serv_language, url);
    } catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        logger.debug("Get rate limited : {}", e);
        event.reply(LanguageManager.getText(server.serv_language, "statsProfileRateLimitError"));
        return;
      }
      logger.warn("Got a unexpected error : {}", e);
      event.reply(LanguageManager.getText(server.serv_language, "statsProfileUnexpectedError"));
      return;
    }

    MessageBuilder messageBuilder = new MessageBuilder();

    messageBuilder.setEmbed(embed);

    if(imageBytes != null) {
      if(player != null) {
        event.getTextChannel().sendMessage(messageBuilder.build()).addFile(imageBytes, player.getUser().getId() + ".png").queue();
      }else {
        event.getTextChannel().sendMessage(messageBuilder.build()).addFile(imageBytes, url + ".png").queue();
      }
    }else {
      event.getTextChannel().sendMessage(messageBuilder.build()).queue();
    }
  }

  private byte[] generateMasteriesChart(DTO.Player player, SavedChampionMastery championsMasteries,
      DTO.Server server, LeagueAccount leagueAccount) throws IOException, RiotApiException {
    List<SavedSimpleMastery> listHeigherChampion = getBestMasteries(championsMasteries, NUMBER_OF_CHAMPIONS_IN_GRAPH);
    CategoryChartBuilder masteriesGraphBuilder = new CategoryChartBuilder();

    masteriesGraphBuilder.chartTheme = ChartTheme.GGPlot2;

    if(player != null) {
      masteriesGraphBuilder.title(String.format(LanguageManager.getText(server.serv_language, "statsProfileGraphTitle"),
          player.getUser().getName()));
    }else {
      masteriesGraphBuilder.title(String.format(LanguageManager.getText(server.serv_language, "statsProfileGraphTitle"),
          leagueAccount.getSummoner().getName()));
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

    masteriesGraph.setXAxisTitle(LanguageManager.getText(server.serv_language, "statsProfileGraphTitleX"));
    masteriesGraph.setYAxisTitle(LanguageManager.getText(server.serv_language, "statsProfileGraphTitleY"));

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

  private long getMoyenneMasteries(List<SavedSimpleMastery> championsMasteries) {
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

  public static List<SavedSimpleMastery> getBestMasteries(SavedChampionMastery championsMasteries, int nbrTop) {
    List<SavedSimpleMastery> listHeigherChampion = new ArrayList<>();

    for(int i = 0; i < nbrTop; i++) {

      SavedSimpleMastery heigherActual = null;

      for(SavedSimpleMastery championMastery : championsMasteries.getChampionMasteries()) {

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

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
