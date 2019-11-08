package ch.kalunight.zoe.command.delete;

import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.static_data.SpellingLangage;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class DeleteInfoChannelCommand extends ZoeCommand {

  public DeleteInfoChannelCommand() {
    this.name = "infoChannel";
    this.arguments = "";
    this.help = "deleteInfoChannelHelpMessage";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeleteCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();

    Server server = ServerData.getServers().get(event.getGuild().getId());

    if(server == null) {
      server = new Server(event.getGuild(), SpellingLangage.EN, new ServerConfiguration());
    }

    if(server.getInfoChannel() == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "deleteInfoChannelChannelNotSetted"));
    } else {
      try {
        server.getInfoChannel().delete().queue();
      } catch(InsufficientPermissionException e) {
        server.setInfoChannel(null);
        server.setControlePannel(new ControlPannel());
        event.reply(LanguageManager.getText(server.getLangage(), "deleteInfoChannelDeletedMissingPermission"));
        return;
      }

      server.setInfoChannel(null);
      server.setControlePannel(new ControlPannel());
      event.reply(LanguageManager.getText(server.getLangage(), "deleteInfoChannelDoneMessage"));
    }
  }
}
