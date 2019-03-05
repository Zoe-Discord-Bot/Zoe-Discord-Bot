package ch.kalunight.zoe.command.create;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;

public class CreateInfoChannel extends Command {

  public CreateInfoChannel() {
    this.name = "InfoChannel";
    this.arguments = "*nameOfTheNewChannel*";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Create a new InfoChannel where Zoe can send info about players";
  }

  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().queue();
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
      Channel infoChannel = event.getGuild().getController().createTextChannel(nameChannel).complete();
      String id = infoChannel.getId();
      TextChannel textChannel = event.getGuild().getTextChannelById(id);
      server.setInfoChannel(textChannel);
      event.reply("The channel got created !");
    }catch(InsufficientPermissionException e) {
      event.reply("Impossible to create the infoChannel ! "
          + "I don't have the permission to do that. Give me the Manage Channel Permission or use `>defineInfoChannel *#MentionOfTheChannel*`.");
    }

  }

}
