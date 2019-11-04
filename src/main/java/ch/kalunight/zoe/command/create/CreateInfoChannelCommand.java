package ch.kalunight.zoe.command.create;

import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.service.InfoPanelRefresher;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class CreateInfoChannelCommand extends ZoeCommand {

  public CreateInfoChannelCommand() {
    this.name = "InfoChannel";
    this.arguments = "nameOfTheNewChannel";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Create a new InfoChannel where i can send info about players. Manage Channel permission needed.";
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    Server server = ServerData.getServers().get(event.getGuild().getId());

    String nameChannel = event.getArgs();

    if(nameChannel == null || nameChannel.equals("")) {
      event.reply("Please give a name for the channel in the command");
      return;
    }

    if(nameChannel.length() > 100) {
      event.reply("Please give me a name smaller than 100 characters");
      return;
    }

    if(server.getInfoChannel() != null) {
      event.reply(server.getInfoChannel().getAsMention() + " already exist, please delete it first");
      return;
    }

    try {
      TextChannel infoChannel = event.getGuild().createTextChannel(nameChannel).complete();
      String id = infoChannel.getId();
      TextChannel textChannel = event.getGuild().getTextChannelById(id);
      server.setInfoChannel(textChannel);
      
      if(server.getControlePannel().getInfoPanel().isEmpty()) {
        server.getControlePannel().getInfoPanel()
        .add(server.getInfoChannel().sendMessage("__**Information Panel**__\n \n*Loading...*").complete());
      }
      
      Runnable task = new InfoPanelRefresher(server);
      ServerData.getServerExecutor().execute(task);

      event.reply("The channel got created !");
    } catch(InsufficientPermissionException e) {
      event.reply("Impossible to create the infoChannel ! "
          + "I don't have the permission to do that. Give me the Manage Channel Permission or use `>defineInfoChannel #MentionOfTheChannel`.");
    }
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Create infoChannel command :\n");
        stringBuilder.append("--> `>create " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
}
