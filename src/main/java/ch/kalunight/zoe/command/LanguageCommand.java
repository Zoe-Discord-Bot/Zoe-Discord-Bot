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
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.LanguageUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class LanguageCommand extends ZoeCommand{

  public static final String NATIVE_LANGUAGE_TRANSLATION_ID = "nativeActualLanguage";
  
  private EventWaiter waiter;
  
  public LanguageCommand(EventWaiter waiter) {
    this.name = "language";
    String[] aliasesTable = {"lang", "l", "languages"};
    this.aliases = aliasesTable;
    this.help = "languageCommandHelp";
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL, Permission.MESSAGE_ADD_REACTION};
    this.userPermissions = permissionRequired;
    Permission[] permissionBot = {Permission.MESSAGE_ADD_REACTION, Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS};
    this.botPermissions = permissionBot;
    this.guildOnly = true;
    this.waiter = waiter;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    event.getTextChannel().sendMessage(String.format(LanguageManager.getText(server.serv_language,
        "languageCommandStartMessage"), LanguageManager.getText(server.serv_language, NATIVE_LANGUAGE_TRANSLATION_ID), "<https://discord.gg/AyAYWGM>")).complete();
    
    SelectionDialog.Builder builder = new SelectionDialog.Builder()
        .addUsers(event.getAuthor())
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
    
    builder.setText(LanguageUtil.getUpdateMessageAfterChangeSelectAction(server.serv_language, languageListTranslate));
    builder.setSelectionConsumer(getSelectionDoneAction(langagesList, server));
    builder.setCanceled(LanguageUtil.getCancelActionSelection());
    
    builder.build().display(event.getChannel());
  }
  
  private BiConsumer<Message, Integer> getSelectionDoneAction(List<String> languageList, DTO.Server server) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfLanguage) {

        selectionMessage.clearReactions().queue();

        try {
          ServerRepository.updateLanguage(server.serv_guildId, languageList.get(selectionOfLanguage - 1));
          server.serv_language = languageList.get(selectionOfLanguage - 1);
        } catch (SQLException e) {
          RepoRessources.sqlErrorReport(selectionMessage.getChannel(), server, e);
          return;
        }
        
        selectionMessage.getTextChannel().sendMessage(String.format(LanguageManager.getText(server.serv_language, "languageCommandSelected"),
            LanguageManager.getText(server.serv_language, NATIVE_LANGUAGE_TRANSLATION_ID))).queue();
      }
    };
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
