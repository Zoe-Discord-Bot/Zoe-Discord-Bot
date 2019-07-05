package ch.kalunight.zoe.command;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.ServerConfiguration;
import net.dv8tion.jda.core.Permission;

public class ConfigCommand extends Command{
  
  private EventWaiter waiter;
  
  public ConfigCommand(EventWaiter waiter) {
    this.name = "config";
    this.help = "Open an interactive message to configure the server.";
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.waiter = waiter;
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    
    OrderedMenu.Builder builder = new OrderedMenu.Builder()
        .addUsers(event.getAuthor())
        .allowTextInput(false)
        .setTimeout(2, TimeUnit.MINUTES)
        .useNumbers()
        .setText("Configuration choices:")
        .setDescription("Configuration Choices")
        .useCancelButton(true);
    
    ServerConfiguration serverConfiguration = ServerData.getServers().get(event.getGuild().getId()).getConfig();
    
    String choice = "Everyone can register them self : ";
    if(serverConfiguration.isUserSelfAdding()) {
      choice += "Activated";
    }else {
      choice += "Disable";
    }
    
    builder.addChoice(choice);
    
    choice = "Players can join/leave allowed team them self : ";
    if(serverConfiguration.isEveryoneCanMoveOfTeam()) {
      choice += "Activated";
    }else {
      choice += "Disable";
    }
    
    builder.addChoice(choice);
    
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name + " command :\n");
        stringBuilder.append("--> `>" + name + " " + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
  
}
