package ch.kalunight.zoe.model.leaderboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;

import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.model.leaderboard.dataholder.SpecificChamp;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.PaginatorUtil;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;

public class SpecificChampionObjectiveDataHandler extends LeaderboardExtraDataHandler {

  private static final String LEADERBOARD_CHAMPION_SELECTED_THEN_SEND_CHANNEL = "leaderboardChampionSelectedThenSendChannel";
  private static final String LEADERBOARD_BAD_CHAMPION_NUMBER_SELECTION = "leaderboardBadChampionNumberSelection";

  public SpecificChampionObjectiveDataHandler(Objective objective, EventWaiter waiter, Server server, boolean forceRefreshCache, Member author, TextChannel channel) {
    super(objective, waiter, server, forceRefreshCache, author, channel);
  }

  @Override
  public void handleSecondCreationPart() {
    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "leaderboardDataNeededSpecificChamp")).queue();
    
    List<String> championsName = new ArrayList<>();
    for(Champion champion : Ressources.getChampions()) {
      championsName.add(champion.getName());
    }
    
    Collections.sort(championsName);
    
    Paginator.Builder pbuilder = new Paginator.Builder()
        .setColumns(1)
        .setItemsPerPage(30)
        .showPageNumbers(true)
        .waitOnSinglePage(false)
        .useNumberedItems(true)
        .setFinalAction(m -> {
          try {
            m.clearReactions().queue();
          } catch(PermissionException ex) {
            m.delete().queue();
          }
        })
        .setEventWaiter(waiter)
        .setUsers(author.getUser())
        .setColor(Color.GREEN)
        .setText(PaginatorUtil.getPaginationTranslatedPage(server.getLanguage()))
        .setText(LanguageManager.getText(server.getLanguage(), "paginationChampionSelection"))
        .setTimeout(1, TimeUnit.MINUTES);
    
    for(String championName : championsName) {
      String championEmote = "";
      
      for(Champion champion : Ressources.getChampions()) {
        if(champion.getName().equalsIgnoreCase(championName)) {
          championEmote = champion.getEmoteUsable() + " ";
        }
      }
      
      pbuilder.addItems(championEmote + championName);
    }
    
    Paginator listChampions = pbuilder.build();
    
    listChampions.display(channel);
    
    waitForAChampionSelection(server, objective, championsName);
  }

  private void cancelSelectionOfAChampion(TextChannel textChannel, Server server) {
    textChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "createLeaderboardCancelMessage")).queue();
  }

  private void threatChampionSelection(MessageReceivedEvent message, Server server, Objective objective,
      List<String> championsNameOrdered) {
    
    if(message.getMessage().getContentRaw().equalsIgnoreCase("Stop")) {
      cancelSelectionOfAChampion(message.getTextChannel(), server);
      return;
    }
    
    try {
      int championNumber = Integer.parseInt(message.getMessage().getContentRaw());
      if(championNumber >= 1 && championNumber < championsNameOrdered.size()) {
        String championNameSelected = championsNameOrdered.get(championNumber - 1);
        
        selectChampionWithName(message, server, objective, championsNameOrdered, championNameSelected);
      }else {
        message.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(),
            LEADERBOARD_BAD_CHAMPION_NUMBER_SELECTION)).queue();
        waitForAChampionSelection(server, objective, championsNameOrdered);
      }
    }catch(NumberFormatException e) {
      selectChampionWithName(message, server, objective, championsNameOrdered, message.getMessage().getContentRaw());
    }
  }

  private void selectChampionWithName(MessageReceivedEvent message, Server server, Objective objective,
      List<String> championsNameOrdered, String championName) {
    Champion selectedChampion = null;
    for(Champion champion : Ressources.getChampions()) {
      if(championName.equalsIgnoreCase(champion.getName())) {
        selectedChampion = champion;
      }
    }
    
    if(selectedChampion == null) {
      message.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(),
          LEADERBOARD_BAD_CHAMPION_NUMBER_SELECTION)).queue();
      waitForAChampionSelection(server, objective, championsNameOrdered);
    }else {
      message.getTextChannel().sendMessage(String.format(LanguageManager.getText(server.getLanguage(),
          LEADERBOARD_CHAMPION_SELECTED_THEN_SEND_CHANNEL), selectedChampion.getName())).queue();
      SpecificChamp dataObject = new SpecificChamp(selectedChampion);
      String dataStringJson = gson.toJson(dataObject);
      handleEndOfCreation(dataStringJson);
    }
  }

  private void waitForAChampionSelection(Server server, Objective objective, List<String> championsNameOrdered) {
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(author.getUser()) && e.getChannel().getId().equals(channel.getId()),
        e -> threatChampionSelection(e, server, objective, championsNameOrdered), 3, TimeUnit.MINUTES,
        () -> cancelSelectionOfAChampion(channel, server));
  }

}
