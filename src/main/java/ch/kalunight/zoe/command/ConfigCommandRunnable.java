package ch.kalunight.zoe.command;

import java.awt.Color;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.OrderedMenu;

import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.ConfigurationOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;

public class ConfigCommandRunnable {
  
  private ConfigCommandRunnable() {
    // hide default constructor
  }
  
  public static void executeCommand(Server server, EventWaiter waiter, CommandGuildDiscordData event) throws SQLException {
    
    OrderedMenu.Builder builder = new OrderedMenu.Builder()
        .addUsers(event.getUser())
        .allowTextInput(false)
        .setTimeout(2, TimeUnit.MINUTES)
        .useNumbers()
        .setColor(Color.BLUE)
        .setText(LanguageManager.getText(server.getLanguage(), "configCommandMenuText"))
        .setDescription(LanguageManager.getText(server.getLanguage(), "configCommandMenuDescription"))
        .useCancelButton(true)
        .setEventWaiter(waiter);
    
    ServerConfiguration serverConfiguration = ConfigRepository.getServerConfiguration(event.getGuild().getIdLong(), event.getChannel().getJDA());
    
    List<ConfigurationOption> options = serverConfiguration.getAllConfigurationOption();
    for(ConfigurationOption option : options) {
      builder.addChoice(option.getChoiceText(server.getLanguage()));
    }
    
    builder.setSelection(getSelectionAction(options, waiter, event))
    .setCancel(getCancelAction(server.getLanguage()));
    
    builder.build().display(event.getChannel());
  }
  
  private static BiConsumer<Message, Integer> getSelectionAction(List<ConfigurationOption> options, EventWaiter waiter, CommandGuildDiscordData event){
    return new BiConsumer<Message, Integer>() {
      
      @Override
      public void accept(Message messageEmbended, Integer selectionNumber) {
        DTO.Server server = ZoeCommand.getServer(event.getGuild().getIdLong());
        options.get(selectionNumber - 1).getChangeConsumer(waiter, server).accept(event);
      }};
  }
  
  private static Consumer<Message> getCancelAction(String language){
    return new Consumer<Message>() {

      @Override
      public void accept(Message message) {
        message.getChannel().sendMessage(LanguageManager.getText(language, "configurationEnded")).queue();
      }};
  }
}
