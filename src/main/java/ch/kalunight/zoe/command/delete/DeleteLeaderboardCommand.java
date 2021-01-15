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
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.repositories.LeaderboardRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class DeleteLeaderboardCommand extends ZoeCommand {

  private static final Logger logger = LoggerFactory.getLogger(DeleteLeaderboardCommand.class);

  private EventWaiter eventWaiter;

  public DeleteLeaderboardCommand(EventWaiter waiter) {
    this.name = "leaderboard";
    String[] aliases = {"leader", "lb", "lead", "board"};
    this.aliases = aliases;
    this.arguments = "";
    this.eventWaiter = waiter;
    this.help = "deleteLeaderboardCommandHelpMessage";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeleteCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    DTO.Server server = getServer(event.getGuild().getIdLong());

    List<Leaderboard> leaderboardList = LeaderboardRepository.getLeaderboardsWithGuildId(server.serv_guildId);
    List<Leaderboard> leaderboardChoiceInOrder = new ArrayList<>();
    AtomicBoolean actionDone = new AtomicBoolean(false);
    
    if(leaderboardList.isEmpty()) {
      event.reply(LanguageManager.getText(server.getLanguage(), "deleteLeaderboardCommandNoLeaderboardToDelete"));
      return;
    }

    SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
        .addUsers(event.getAuthor())
        .setEventWaiter(eventWaiter)
        .useLooping(true)
        .setColor(Color.GREEN)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server.getLanguage(), actionDone))
        .setSelectionConsumer(getSelectionConsumer(server, event, leaderboardList, actionDone))
        .setTimeout(2, TimeUnit.MINUTES);

    for(Leaderboard leaderboard : leaderboardList) {
      TextChannel channel = event.getGuild().getTextChannelById(leaderboard.lead_message_channelId);
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
    choiceLeaderBoard.display(event.getChannel());
  }

  private Consumer<Message> getSelectionCancelAction(String language, AtomicBoolean actionDone){
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

  private BiConsumer<Message, Integer> getSelectionConsumer(Server server, CommandEvent event, List<Leaderboard> leaderboardList, AtomicBoolean actionDone) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer objectiveSelection) {
        event.getChannel().sendTyping().queue();
        selectionMessage.clearReactions().queue();
        actionDone.set(true);

        Leaderboard leaderboard = leaderboardList.get(objectiveSelection - 1);

        event.getGuild().getTextChannelById(leaderboard.lead_message_channelId).deleteMessageById(leaderboard.lead_message_id).queue();

        try {
          LeaderboardRepository.deleteLeaderboardWithId(leaderboard.lead_id);
        } catch(SQLException e) {
          logger.error("SQL Exception throw while deleted a leaderboard !", e);
          event.reply(LanguageManager.getText(server.getLanguage(), "deleteLeaderboardErrorDatabase"));
          return;
        }

        event.reply(LanguageManager.getText(server.getLanguage(), "deleteLeaderboardDoneCorrectly"));
      }
    };
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
