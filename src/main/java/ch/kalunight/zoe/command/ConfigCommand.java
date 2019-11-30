package ch.kalunight.zoe.command;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.ConfigurationOption;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class ConfigCommand extends ZoeCommand {
  
  private EventWaiter waiter;
  
  public ConfigCommand(EventWaiter waiter) {
    this.name = "config";
    this.help = "configCommandHelp";
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
    
    OrderedMenu.Builder builder = new OrderedMenu.Builder()
        .addUsers(event.getAuthor())
        .allowTextInput(false)
        .setTimeout(2, TimeUnit.MINUTES)
        .useNumbers()
        .setColor(Color.BLUE)
        .setText(LanguageManager.getText(server.getLangage(), "configCommandMenuText"))
        .setDescription(LanguageManager.getText(server.getLangage(), "configCommandMenuDescription"))
        .useCancelButton(true)
        .setEventWaiter(waiter);
    
    ServerConfiguration serverConfiguration = server.getConfig();
    
    List<ConfigurationOption> options = serverConfiguration.getAllConfigurationOption();
    for(ConfigurationOption option : options) {
      builder.addChoice(option.getChoiceText(server.getLangage()));
    }
    
    builder.setSelection(getSelectionAction(options, event))
    .setCancel(getCancelAction(server.getLangage()));
    
    builder.build().display(event.getChannel());
  }
  
  private BiConsumer<Message, Integer> getSelectionAction(List<ConfigurationOption> options, CommandEvent event){
    return new BiConsumer<Message, Integer>() {
      
      @Override
      public void accept(Message messageEmbended, Integer selectionNumber) {
        options.get(selectionNumber - 1).getChangeConsumer(waiter).accept(event);
      }};
  }
  
  private Consumer<Message> getCancelAction(String language){
    return new Consumer<Message>() {

      @Override
      public void accept(Message message) {
        message.getChannel().sendMessage(LanguageManager.getText(language, "configurationEnded")).queue();
      }};
  }
}
