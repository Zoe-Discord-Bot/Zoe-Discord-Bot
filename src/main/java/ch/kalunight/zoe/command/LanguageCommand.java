package ch.kalunight.zoe.command;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.static_data.SpellingLanguage;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class LanguageCommand extends ZoeCommand{

  private EventWaiter waiter;
  
  public LanguageCommand(EventWaiter waiter) {
    this.name = "language";
    String[] aliasesTable = {"lang", "l", "languages"};
    this.aliases = aliasesTable;
    this.help = "LanguageCommandHelp";
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
    
    String message = String.format(LanguageManager.getText(server.getLangage(),
        "languageCommandStartMessage"), server.getLangage().nameInNativeLanguage(), "https://discord.gg/XuZAfGK");
    
    OrderedMenu.Builder builder = new OrderedMenu.Builder()
        .addUsers(event.getAuthor())
        .allowTextInput(false)
        .setTimeout(2, TimeUnit.MINUTES)
        .useNumbers()
        .setColor(Color.GREEN)
        .setText(LanguageManager.getText(server.getLangage(), message))
        .setDescription(LanguageManager.getText(server.getLangage(), "languageCommandMenuDescription"))
        .useCancelButton(true)
        .setEventWaiter(waiter);
    
    List<SpellingLanguage> langagesList = new ArrayList<SpellingLanguage>();
    for(SpellingLanguage langage : SpellingLanguage.values()) {
      builder.addChoices(langage.nameInNativeLanguage());
      langagesList.add(langage);
    }
    
    builder.setSelection(getSelectionAction(langagesList, event, server));
    builder.setCancel(getCancelAction(server.getLangage()));
    
    builder.build().display(event.getChannel());
  }
  
  private BiConsumer<Message, Integer> getSelectionAction(List<SpellingLanguage> langages, CommandEvent event, Server server){
    return new BiConsumer<Message, Integer>() {
      
      @Override
      public void accept(Message messageEmbended, Integer selectionNumber) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        server.setLangage(langages.get(selectionNumber - 1));
        event.reply(String.format(
            LanguageManager.getText(server.getLangage(), "languageCommandSelected"), server.getLangage().nameInNativeLanguage()));
      }};
  }
  
  private Consumer<Message> getCancelAction(SpellingLanguage langage){
    return new Consumer<Message>() {

      @Override
      public void accept(Message message) {
        message.getChannel().sendMessage(LanguageManager.getText(langage, "languageCommandSelectionEnded")).queue();
      }};
  }
}
