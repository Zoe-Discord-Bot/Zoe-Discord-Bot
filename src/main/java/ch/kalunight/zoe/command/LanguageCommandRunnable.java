package ch.kalunight.zoe.command;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.LanguageUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class LanguageCommandRunnable {  
  
  public static final String NATIVE_LANGUAGE_TRANSLATION_ID = "nativeActualLanguage";
  
  private LanguageCommandRunnable() {
    //Hide default public constructor
  }
  
  public static SelectionDialog executeCommand(Server server, EventWaiter waiter, User author) {
    
    SelectionDialog.Builder builder = new SelectionDialog.Builder()
        .addUsers(author)
        .setTimeout(2, TimeUnit.MINUTES)
        .setColor(Color.GREEN)
        .useLooping(true)
        .setSelectedEnds("**", "**")
        .setEventWaiter(waiter);
    
    List<String> langagesList = new ArrayList<>();
    List<String> languageListTranslate = new ArrayList<>();
    for(String langage : LanguageManager.getListlanguages()) {
      builder.addChoices(LanguageManager.getText(langage, NATIVE_LANGUAGE_TRANSLATION_ID) 
          + " " + LanguageManager.getPourcentageTranslated(langage));
      languageListTranslate.add(LanguageManager.getText(langage, NATIVE_LANGUAGE_TRANSLATION_ID));
      langagesList.add(langage);
    }
    
    builder.setText(LanguageUtil.getUpdateMessageAfterChangeSelectAction(server.getLanguage(), languageListTranslate, server));
    builder.setSelectionConsumer(getSelectionDoneAction(langagesList, server));
    builder.setCanceled(LanguageUtil.getCancelActionSelection());
    
    return builder.build();
  }
  
  private static BiConsumer<Message, Integer> getSelectionDoneAction(List<String> languageList, DTO.Server server) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfLanguage) {

        selectionMessage.clearReactions().queue();

        try {
          ServerRepository.updateLanguage(server.serv_guildId, languageList.get(selectionOfLanguage - 1));
          server.setLanguage(languageList.get(selectionOfLanguage - 1));
        } catch (SQLException e) {
          RepoRessources.sqlErrorReport(selectionMessage.getChannel(), server, e);
          return;
        }
        
        selectionMessage.getTextChannel().sendMessage(String.format(LanguageManager.getText(server.getLanguage(), "languageCommandSelected"),
            LanguageManager.getText(server.getLanguage(), NATIVE_LANGUAGE_TRANSLATION_ID))).queue();
      }
    };
  }


}
