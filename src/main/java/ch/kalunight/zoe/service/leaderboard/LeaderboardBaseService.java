package ch.kalunight.zoe.service.leaderboard;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.repositories.LeaderboardRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import ch.kalunight.zoe.util.ZoeSupportMessageGeneratorUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.rithms.riot.api.RiotApiException;

public abstract class LeaderboardBaseService implements Runnable {

  protected static final DecimalFormat masteryPointsFormat = new DecimalFormat("###,###,###");

  protected static final Logger logger = LoggerFactory.getLogger(LeaderboardBaseService.class);

  protected static final Gson gson = new GsonBuilder().create();

  private static final int NUMBER_OF_MESSAGE_CONSIDERED_VISIBLE = 20;

  private static final int NUMBER_OF_MESSAGE_FOR_DELETION = 50;

  private static final int MAX_PLAYERS_IN_LEADERBOARD = 10;

  private static final int CHANCE_RANDOM_MAX_RANGE = 10;

  private static final int CHANCE_TO_SHOW_SOMETHING = 1; // 1 chance on 10

  private static final Random rand = new Random();

  private long guildId;

  private long channelId;

  private long leaderboardId;

  private boolean forceRefreshCache;

  public LeaderboardBaseService(long guildId, long channelId, long leaderboardId, boolean forceRefreshCache) {
    this.guildId = guildId;
    this.channelId = channelId;
    this.leaderboardId = leaderboardId;
    this.forceRefreshCache = forceRefreshCache;
  }

  @Override
  public void run() {
    try {

      Server server = ServerRepository.getServerWithGuildId(guildId);

      Guild guild = Zoe.getGuildById(guildId);
      if(guild == null) {
        return;
      }

      TextChannel channel = guild.getTextChannelById(channelId);
      if(channel == null) {
        deleteLeaderboard();
        return;
      }

      Leaderboard leaderboard = LeaderboardRepository.getLeaderboardWithId(leaderboardId);

      Message message = channel.retrieveMessageById(leaderboard.lead_message_id).complete();

      int messageAfterTheLeaderboard = messageAfterTheLeaderboard(channel, message);

      if(messageAfterTheLeaderboard > NUMBER_OF_MESSAGE_CONSIDERED_VISIBLE
          && messageAfterTheLeaderboard < NUMBER_OF_MESSAGE_FOR_DELETION) {
        message.editMessage(LanguageManager.getText(server.getLanguage(), "leaderboardNotRefreshedMessage")).queue();
        return;

      }else if (messageAfterTheLeaderboard > NUMBER_OF_MESSAGE_FOR_DELETION) {
        message.editMessage(LanguageManager.getText(server.getLanguage(), "leaderboardDeletedMessage")).queue();
        LeaderboardRepository.deleteLeaderboardWithId(leaderboardId);
        return;
      }

      message.addReaction("U+23F3").queue();

      List<Player> players = PlayerRepository.getPlayers(guildId);
      InfoPanelRefresherUtil.cleanRegisteredPlayerNoLongerInGuild(guild, players);

      runLeaderboardRefresh(server, guild, channel, leaderboard, message, players, forceRefreshCache);

      message.removeReaction("U+23F3", guild.getJDA().getSelfUser()).queue();

    }catch(ErrorResponseException e) {
      if(e.getErrorResponse().equals(ErrorResponse.UNKNOWN_MESSAGE)) {
        logger.warn("leaderboard message has been deleted, delete db stats");
        deleteLeaderboard();
      }else {
        logger.error("Error while getting discord data", e);
      }
    }catch(SQLException e) {
      logger.error("Error while accessing to the DB", e);
    }catch(Exception e) {
      logger.error("Unexpected error when refreshing leaderboard", e);
    }
  }

  private int messageAfterTheLeaderboard(TextChannel channel, Message message) {
    return channel.getHistoryAfter(message, NUMBER_OF_MESSAGE_FOR_DELETION + 1).complete().size();
  }

  private void deleteLeaderboard() {
    try {
      LeaderboardRepository.deleteLeaderboardWithId(leaderboardId);
    } catch(SQLException e) {
      logger.error("Error while deleting leaderboard !", e);
    }
  }

  protected abstract void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel,
      Leaderboard leaderboard, Message message, List<Player> players, boolean forceRefreshCache) throws SQLException, RiotApiException;

  protected EmbedBuilder buildBaseLeaderboardList(String playerTitle, List<String> playersName, String dataName, List<String> dataList,
      Server server) {

    EmbedBuilder builder = new EmbedBuilder();

    StringBuilder stringListPlayer = new StringBuilder();

    for(int i = 0; i < playersName.size(); i++) {
      if(i == 0) {
        stringListPlayer.append("🥇 " + playersName.get(i));
      }else if(i == 1) {
        stringListPlayer.append("🥈 " + playersName.get(i));
      }else if (i == 2) {
        stringListPlayer.append("🥉 " + playersName.get(i));
      }else if (i < MAX_PLAYERS_IN_LEADERBOARD) {
        stringListPlayer.append((i + 1) + ". " + playersName.get(i));
      }else {
        break;
      }

      if((i + 1) < playersName.size()) {
        stringListPlayer.append("\n");
      }
    }

    Field field = new Field(playerTitle, stringListPlayer.toString(), true);
    builder.addField(field);

    StringBuilder stringData = new StringBuilder();

    for(int i = 0; i < dataList.size(); i++) {
      if(i < MAX_PLAYERS_IN_LEADERBOARD) {
        stringData.append(dataList.get(i));
      }else {
        break;
      }

      if((i + 1) < playersName.size()) {
        stringData.append("\n");
      }
    }

    field = new Field(dataName, stringData.toString(), true);
    builder.addField(field);

    int randomNumber = rand.nextInt(CHANCE_RANDOM_MAX_RANGE);
    if(randomNumber < CHANCE_TO_SHOW_SOMETHING) {
      field = new Field("", 
          "*" + ZoeSupportMessageGeneratorUtil.getRandomSupportPhrase(server.getLanguage()) + "*",
          false);
      builder.addField(field);
    }

    builder.setFooter(LanguageManager.getText(server.getLanguage(), "leaderboardRefreshMessage"));

    return builder;
  }

  public static LeaderboardBaseService getServiceWithObjective(Objective objective, long guildId, long channelId, long leaderboardId, boolean forceRefreshCache) {

    switch(objective) {
    case AVERAGE_KDA:
    case AVERAGE_KDA_SPECIFIC_CHAMP:
      return new KDALeaderboardService(guildId, channelId, leaderboardId, forceRefreshCache);
    case MASTERY_POINT:
      return new MasteryPointLeaderboardService(guildId, channelId, leaderboardId, forceRefreshCache);
    case MASTERY_POINT_SPECIFIC_CHAMP:
      return new MasteryPointSpecificChampLeaderboardService(guildId, channelId, leaderboardId, forceRefreshCache);
    case BEST_OF_ALL_RANK:
    case SPECIFIC_QUEUE_RANK:
      return new RankLeaderboardService(guildId, channelId, leaderboardId, forceRefreshCache);
      /*case WINRATE:
    case WINRATE_SPECIFIC_CHAMP:
    case WINRATE_SPECIFIC_QUEUE:
      return new WinrateLeaderboardService(guildId, channelId, leaderboardId);*/
    default:
      break;
    }

    return null;
  }
}
