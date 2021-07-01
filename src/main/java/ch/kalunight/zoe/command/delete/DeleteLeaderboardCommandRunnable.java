package ch.kalunight.zoe.command.delete;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.repositories.LeaderboardRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class DeleteLeaderboardCommandRunnable {

  private static final Logger logger = LoggerFactory.getLogger(DeleteLeaderboardCommandRunnable.class);

  private DeleteLeaderboardCommandRunnable() {
    // hide default public constructor
  }
  
  public static void executeCommand(Server server, Member author, TextChannel commandOriginChannel, Message toEdit, EventWaiter waiter, InteractionHook hook) throws SQLException {

    List<Leaderboard> leaderboardList = LeaderboardRepository.getLeaderboardsWithGuildId(server.serv_guildId);
    List<Leaderboard> leaderboardChoiceInOrder = new ArrayList<>();
    AtomicBoolean actionDone = new AtomicBoolean(false);
    
    if(leaderboardList.isEmpty()) {
      String message = LanguageManager.getText(server.getLanguage(), "deleteLeaderboardCommandNoLeaderboardToDelete");
      if(toEdit != null) {
        toEdit.editMessage(message).queue();
      }else {
        hook.editOriginal(message).queue();
      }
      return;
    }

    SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
        .addUsers(author.getUser())
        .setEventWaiter(waiter)
        .useLooping(true)
        .setColor(Color.GREEN)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server.getLanguage(), actionDone))
        .setSelectionConsumer(getSelectionConsumer(server, leaderboardList, actionDone, commandOriginChannel))
        .setTimeout(2, TimeUnit.MINUTES);

    for(Leaderboard leaderboard : leaderboardList) {
      TextChannel channel = commandOriginChannel.getGuild().getTextChannelById(leaderboard.lead_message_channelId);
      try {
        channel.retrieveMessageById(leaderboard.lead_message_id).complete();
      }catch(ErrorResponseException e) {
        if(e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
          logger.info("Message of this leaderboard has been deleted, refresh db stats...");
          LeaderboardRepository.deleteLeaderboardWithId(leaderboard.lead_id);
          continue;
        }
      }

      String showableString = Objective.getShowableDeletionFormat(leaderboard, server.getLanguage(), channel);

      leaderboardChoiceInOrder.add(leaderboard);
      selectAccountBuilder.addChoices(showableString);
    }

    selectAccountBuilder.setText(LanguageManager.getText(server.getLanguage(), "deleteLeaderboardCommandListProposal"));

    SelectionDialog choiceLeaderBoard = selectAccountBuilder.build();
    choiceLeaderBoard.display(commandOriginChannel);
  }

  private static Consumer<Message> getSelectionCancelAction(String language, AtomicBoolean actionDone){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        if(!actionDone.get()) {
          message.clearReactions().queue();
          message.editMessage(LanguageManager.getText(language, "deleteLeaderboardCancelMessage")).queue();
        }
      }
    };
  }

  private static BiConsumer<Message, Integer> getSelectionConsumer(Server server, List<Leaderboard> leaderboardList, AtomicBoolean actionDone, TextChannel channel) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer objectiveSelection) {
        channel.sendTyping().queue();
        selectionMessage.clearReactions().queue();
        actionDone.set(true);

        Leaderboard leaderboard = leaderboardList.get(objectiveSelection - 1);

        channel.getGuild().getTextChannelById(leaderboard.lead_message_channelId).deleteMessageById(leaderboard.lead_message_id).queue();

        try {
          LeaderboardRepository.deleteLeaderboardWithId(leaderboard.lead_id);
        } catch(SQLException e) {
          logger.error("SQL Exception throw while deleted a leaderboard !", e);
          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "deleteLeaderboardErrorDatabase")).queue();
          return;
        }

        channel.sendMessage(LanguageManager.getText(server.getLanguage(), "deleteLeaderboardDoneCorrectly")).queue();
      }
    };
  }

}
