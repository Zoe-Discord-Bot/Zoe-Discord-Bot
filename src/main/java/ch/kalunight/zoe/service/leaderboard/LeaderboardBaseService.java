package ch.kalunight.zoe.service.leaderboard;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.repositories.LeaderboardRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.rithms.riot.api.RiotApiException;

public abstract class LeaderboardBaseService implements Runnable {

  protected static final DecimalFormat masteryPointsFormat = new DecimalFormat("###,###,###");

  protected static final Logger logger = LoggerFactory.getLogger(LeaderboardBaseService.class);
  
  protected static final Gson gson = new GsonBuilder().create();
  
  private static final int MAX_PLAYERS_IN_LEADERBOARD = 10;
  
  private long guildId;

  private long channelId;

  private long leaderboardId;

  public LeaderboardBaseService(long guildId, long channelId, long leaderboardId) {
    this.guildId = guildId;
    this.channelId = channelId;
    this.leaderboardId = leaderboardId;
  }

  @Override
  public void run() {
    try {

      Server server = ServerRepository.getServerWithGuildId(guildId);

      Guild guild = Zoe.getJda().getGuildById(guildId);

      TextChannel channel = guild.getTextChannelById(channelId);

      Leaderboard leaderboard = LeaderboardRepository.getLeaderboardWithId(leaderboardId);

      Message message = channel.retrieveMessageById(leaderboard.lead_message_id).complete();

      message.addReaction("U+23F3").complete();

      runLeaderboardRefresh(server, guild, channel, leaderboard, message);
      
      message.removeReaction("U+23F3", Zoe.getJda().getSelfUser()).queue();

    }catch(ErrorResponseException e) {      
      logger.error("Error while getting discord data", e);
    }catch(SQLException e) {
      logger.error("Error while accessing to the DB", e);
    }catch(Exception e) {
      logger.error("Unexpected error when refreshing leaderboard", e);
    }
  }

  protected abstract void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel,
      Leaderboard leaderboard, Message message) throws SQLException, RiotApiException;

  protected EmbedBuilder buildBaseLeaderboardList(String playerTitle, List<String> playersName, String dataName, List<String> dataList) {

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

    return builder;
  }

  public static LeaderboardBaseService getServiceWithId(Objective objective, long guildId, long channelId, long leaderboardId) {

    switch(objective) {
    case AVERAGE_KDA:
      break;
    case MASTERY_EVERYONE_START_FROM_0:
      break;
    case MASTERY_POINT:
      return new MasteryPointLeaderboardService(guildId, channelId, leaderboardId);
    case MASTERY_POINT_SPECIFIC_CHAMP:
      return new MasteryPointSpecificChampLeaderboardService(guildId, channelId, leaderboardId);
    case MASTERY_POINT_START_FROM_0_SPECIFIC_CHAMP:
      break;
    case SPECIFIC_QUEUE_RANK:
      return new RankLeaderboardService(guildId, channelId, leaderboardId);
    case SPECIFIC_QUEUE_RANK_PROGRESSION:
      break;
    default:
      break;
    }

    return null;
  }
}
