package ch.kalunight.zoe.command.create;

import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.service.InfoPanelRefresher;
import ch.kalunight.zoe.translation.LanguageManager;
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
    this.help = "createInfoChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    Server server = ServerData.getServers().get(event.getGuild().getId());

    String nameChannel = event.getArgs();

    if(nameChannel == null || nameChannel.equals("")) {
      event.reply(LanguageManager.getText(server.getLangage(), "nameOfInfochannelNeeded"));
      return;
    }

    if(nameChannel.length() > 100) {
      event.reply(LanguageManager.getText(server.getLangage(), "nameOfTheInfoChannelNeedToBeLess100Characters"));
      return;
    }

    if(server.getInfoChannel() != null) {
      event.reply(LanguageManager.getText(server.getLangage(), "infochannelAlreadyExist"));
      return;
    }

    try {
      TextChannel infoChannel = event.getGuild().createTextChannel(nameChannel).complete();
      String id = infoChannel.getId();
      TextChannel textChannel = event.getGuild().getTextChannelById(id);
      server.setInfoChannel(textChannel.getIdLong());
      
      if(server.getControlePannel().getInfoPanel().isEmpty()) {
        server.getControlePannel().getInfoPanel()
        .add(server.getInfoChannel().sendMessage(LanguageManager.getText(server.getLangage(), "infoChannelTitle")
            + "\n \n" + LanguageManager.getText(server.getLangage(), "loading")).complete());
      }
      
      Runnable task = new InfoPanelRefresher(server);
      ServerData.getServerExecutor().execute(task);

      event.reply(LanguageManager.getText(server.getLangage(), "channelCreatedMessage"));
    } catch(InsufficientPermissionException e) {
      event.reply(LanguageManager.getText(server.getLangage(), "impossibleToCreateInfoChannelMissingPerms"));
    }
  }
}
