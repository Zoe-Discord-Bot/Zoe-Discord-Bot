package ch.kalunight.zoe.model.leaderboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.model.leaderboard.dataholder.QueueSelected;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;

public class SpecificQueueDataHandler extends LeaderboardExtraDataHandler {

  public SpecificQueueDataHandler(Objective objective, EventWaiter waiter, CommandEvent event, Server server, boolean forceRefreshCache) {
    super(objective, waiter, event, server, forceRefreshCache);
  }

  @Override
  public void handleSecondCreationPart() {
    
    event.reply(LanguageManager.getText(server.serv_language, "leaderboardSelectQueue"));
    
    SelectionDialog.Builder selectQueueBuilder = new SelectionDialog.Builder()
        .addUsers(event.getAuthor())
        .setEventWaiter(waiter)
        .useLooping(true)
        .setColor(Color.GREEN)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server.serv_language))
        .setTimeout(2, TimeUnit.MINUTES);
    
    List<GameQueueConfigId> queueOrder = new ArrayList<>();
    
    for(GameQueueConfigId queue : GameQueueConfigId.values()) {
      selectQueueBuilder.addChoices(LanguageManager.getText(server.serv_language, queue.getNameId()));
      queueOrder.add(queue);
    }
    
    selectQueueBuilder.setSelectionConsumer(getSelectionDoneAction(server.serv_language, queueOrder));
    
    SelectionDialog queueChoiceMenu = selectQueueBuilder.build();
    queueChoiceMenu.display(event.getTextChannel());
  }
  
  private BiConsumer<Message, Integer> getSelectionDoneAction(String language, List<GameQueueConfigId> queueList) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfQueue) {

        selectionMessage.clearReactions().queue();
        
        GameQueueConfigId selectedQueue = queueList.get(selectionOfQueue - 1);
        
        String extraDataNeeded = gson.toJson(new QueueSelected(selectedQueue));

        selectionMessage.getTextChannel().sendMessage(String.format(LanguageManager.getText(language, "leaderboardQueueSelectedThenSendChannel"),
            LanguageManager.getText(server.serv_language, selectedQueue.getNameId()))).queue();
        
        handleEndOfCreation(extraDataNeeded);
      }
    };
  }
  
  private Consumer<Message> getSelectionCancelAction(String language){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
        message.editMessage(LanguageManager.getText(language, "leaderboardCancelSelectionOfChannel")).queue();
      }
    };
  }

}
