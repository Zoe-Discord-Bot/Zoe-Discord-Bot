package ch.kalunight.zoe.model.leaderboard;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.repositories.LeaderboardRepository;
import ch.kalunight.zoe.service.leaderboard.LeaderboardBaseService;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public abstract class LeaderboardExtraDataHandler {

  protected static final Logger logger = LoggerFactory.getLogger(LeaderboardExtraDataHandler.class);
  protected static final Gson gson = new GsonBuilder().create();

  protected Objective objective;
  protected EventWaiter waiter;
  protected Server server;
  protected Member author;
  protected TextChannel channel;
  protected boolean forceRefreshCache;

  public LeaderboardExtraDataHandler(Objective objective, EventWaiter waiter, Server server, boolean forceRefreshCache, Member author, TextChannel channel) {
    this.objective = objective;
    this.waiter = waiter;
    this.server = server;
    this.author = author;
    this.channel = channel;
    this.forceRefreshCache = forceRefreshCache;
  }

  public abstract void handleSecondCreationPart();

  protected void handleEndOfCreation(String dataOftheLeaderboard) {

    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(author.getUser()) && e.getChannel().getId().equals(channel.getId()),
        e -> threatChannelSelection(dataOftheLeaderboard, e), 2, TimeUnit.MINUTES,
        () -> cancelProcedure(channel, server));
  }

  private void threatChannelSelection(String dataOfLeaderboard, MessageReceivedEvent event) {
    event.getTextChannel().sendTyping().queue();
    Message message = event.getMessage();

    TextChannel leaderboardChannel;

    if(event.getMessage().getContentRaw().equalsIgnoreCase("Stop")) {
      event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "leaderboardCancelSelectionOfChannel")).queue();
      return;
    }
    
    if(event.getMessage().getMentionedChannels().size() != 1) {
      message.getChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "createLeaderboardNeedOneMentionnedChannel")).queue();
      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> threatChannelSelection(dataOfLeaderboard, e), 2, TimeUnit.MINUTES,
          () -> cancelProcedure(event.getTextChannel(), server));
      return;
    }

    leaderboardChannel = message.getMentionedChannels().get(0);

    try {
      Message leaderboardMessage = leaderboardChannel.sendMessage(LanguageManager.getText(server.getLanguage(),
          "leaderboardObjectiveBaseMessage")).complete();

      Leaderboard leaderboard = LeaderboardRepository.createLeaderboard(server.serv_id, leaderboardChannel.getIdLong(),
          leaderboardMessage.getIdLong(), objective.getId());

      if(!dataOfLeaderboard.equals("")) {
        LeaderboardRepository.updateLeaderboardDataWithLeadId(leaderboard.lead_id, dataOfLeaderboard);
      }

      LeaderboardBaseService baseLeaderboardService = LeaderboardBaseService.getServiceWithObjective(objective,
          server.serv_guildId, leaderboardChannel.getIdLong(), leaderboard.lead_id, forceRefreshCache);

      ServerThreadsManager.getLeaderboardExecutor().execute(baseLeaderboardService);

      event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "leaderboardSuccessfullyCreated")).queue();
    }catch(ErrorResponseException error) {
      message.getChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "leaderboardMissingPermission")).queue();
      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> threatChannelSelection(dataOfLeaderboard, e), 2, TimeUnit.MINUTES,
          () -> cancelProcedure(event.getTextChannel(), server));
    } catch (SQLException e) {
      event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).queue();
      logger.warn("SQL Error when creating leaderboard", e);
    }
  }

  private void cancelProcedure(TextChannel channel, Server server) {
    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveChannelSelectionTimeOut")).queue();
  }
}
