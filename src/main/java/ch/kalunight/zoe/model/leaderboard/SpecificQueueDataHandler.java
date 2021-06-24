package ch.kalunight.zoe.model.leaderboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.model.leaderboard.dataholder.QueueSelected;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class SpecificQueueDataHandler extends LeaderboardExtraDataHandler {

  public SpecificQueueDataHandler(Objective objective, EventWaiter waiter, Server server, boolean forceRefreshCache, Member author, TextChannel channel) {
    super(objective, waiter, server, forceRefreshCache, author, channel);
  }

  @Override
  public void handleSecondCreationPart() {
    
    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "leaderboardSelectQueue")).queue();
    
    SelectionDialog.Builder selectQueueBuilder = new SelectionDialog.Builder()
        .addUsers(author.getUser())
        .setEventWaiter(waiter)
        .useLooping(true)
        .setColor(Color.GREEN)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server.getLanguage()))
        .setTimeout(2, TimeUnit.MINUTES);
    
    List<GameQueueConfigId> queueOrder = new ArrayList<>();
    
    for(GameQueueConfigId queue : GameQueueConfigId.values()) {
      selectQueueBuilder.addChoices(LanguageManager.getText(server.getLanguage(), queue.getNameId()));
      queueOrder.add(queue);
    }
    
    selectQueueBuilder.setSelectionConsumer(getSelectionDoneAction(server.getLanguage(), queueOrder));
    
    SelectionDialog queueChoiceMenu = selectQueueBuilder.build();
    queueChoiceMenu.display(channel);
  }
  
  private BiConsumer<Message, Integer> getSelectionDoneAction(String language, List<GameQueueConfigId> queueList) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfQueue) {

        selectionMessage.clearReactions().queue();
        
        GameQueueConfigId selectedQueue = queueList.get(selectionOfQueue - 1);
        
        String extraDataNeeded = gson.toJson(new QueueSelected(selectedQueue));

        selectionMessage.getTextChannel().sendMessage(String.format(LanguageManager.getText(language, "leaderboardQueueSelectedThenSendChannel"),
            LanguageManager.getText(server.getLanguage(), selectedQueue.getNameId()))).queue();
        
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
