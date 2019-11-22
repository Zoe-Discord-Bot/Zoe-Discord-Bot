package ch.kalunight.zoe.command;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class LanguageCommand extends ZoeCommand{

  private static final String NATIVE_LANGUAGE_TRANSLATION_ID = "nativeActualLanguage";
  
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
    
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    event.getTextChannel().sendMessage(String.format(LanguageManager.getText(server.getLangage(),
        "languageCommandStartMessage"), LanguageManager.getText(server.getLangage(), NATIVE_LANGUAGE_TRANSLATION_ID), "<https://discord.gg/AyAYWGM>")).complete();
    
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
    
    builder.setText(getUpdateMessageAfterChangeSelectAction(server.getLangage(), languageListTranslate));
    builder.setSelectionConsumer(getSelectionDoneAction(langagesList, server));
    builder.setCanceled(getCancelAction());
    
    builder.build().display(event.getChannel());
  }
  
  private Function<Integer, String> getUpdateMessageAfterChangeSelectAction(String language, List<String> choices) {
    return new Function<Integer, String>() {
      @Override
      public String apply(Integer index) {
        return String.format(LanguageManager.getText(language, "languageCommandInSelectionMenu"), choices.get(index - 1));
      }
    };
  }
  
  private BiConsumer<Message, Integer> getSelectionDoneAction(List<String> languageList, Server server) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfLanguage) {

        selectionMessage.clearReactions().queue();

        server.setLangage(languageList.get(selectionOfLanguage - 1));
        
        selectionMessage.getTextChannel().sendMessage(String.format(LanguageManager.getText(server.getLangage(), "languageCommandSelected"),
            LanguageManager.getText(server.getLangage(), NATIVE_LANGUAGE_TRANSLATION_ID))).queue();
      }
    };
  }
  
  private Consumer<Message> getCancelAction(){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
      }};
  }
}
