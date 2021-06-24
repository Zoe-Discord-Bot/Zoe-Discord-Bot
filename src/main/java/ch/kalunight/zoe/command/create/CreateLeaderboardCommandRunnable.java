package ch.kalunight.zoe.command.create;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.LeaderboardExtraDataHandler;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class CreateLeaderboardCommandRunnable {
  
  private CreateLeaderboardCommandRunnable() {
    //hide default public constructor
  }
  
  public static void executeCommand(Server server, Member author, TextChannel channel, Message messageToEdit, EventWaiter waiter, InteractionHook hook) {

    String messageToSend = LanguageManager.getText(server.getLanguage(), "createLeaderboardExplainMessage");
    
    if(hook == null) {
      messageToEdit.editMessage(messageToSend).queue();
    }else {
      hook.editOriginal(messageToSend).queue();
    }
    
    List<Objective> objectiveList = new ArrayList<>();
    List<String> objectiveChoices = new ArrayList<>();
    AtomicBoolean actionDone = new AtomicBoolean(false);

    SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
        .addUsers(author.getUser())
        .setEventWaiter(waiter)
        .useLooping(true)
        .setColor(Color.GREEN)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server.getLanguage(), actionDone))
        .setSelectionConsumer(getSelectionConsumer(server, objectiveList, actionDone, waiter, author, channel))
        .setTimeout(2, TimeUnit.MINUTES);

    for(Objective objective : Objective.values()) {
      String actualChoice = String.format(LanguageManager.getText(server.getLanguage(), objective.getTranslationId()));

      objectiveChoices.add(actualChoice);
      selectAccountBuilder.addChoices(actualChoice);
      objectiveList.add(objective);
    }

    selectAccountBuilder.setText(LanguageManager.getText(server.getLanguage(), "createLeaderboardTitleListeObjective"));

    SelectionDialog choiceLeaderBoard = selectAccountBuilder.build();
    choiceLeaderBoard.display(channel);
  }

  private static Consumer<Message> getSelectionCancelAction(String language, AtomicBoolean selectionDone){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        if(!selectionDone.get()) {
          message.clearReactions().queue();
          message.editMessage(LanguageManager.getText(language, "createLeaderboardCancelMessage")).queue();
        }
      }
    };
  }

  private static BiConsumer<Message, Integer> getSelectionConsumer(Server server, List<Objective> objectiveList, AtomicBoolean selectionDone, EventWaiter waiter, Member author, TextChannel channel) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer objectiveSelection) {
        selectionMessage.clearReactions().queue();
        selectionDone.set(true);

        Objective objective = objectiveList.get(objectiveSelection - 1);

        channel.sendMessage(String.format(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveSelected"),
            LanguageManager.getText(server.getLanguage(), objective.getTranslationId()))).queue();

        LeaderboardExtraDataHandler dataNeeded = Objective.getDataNeeded(objective, waiter, server, author, channel, true);

        dataNeeded.handleSecondCreationPart();
      }
    };
  }

}
