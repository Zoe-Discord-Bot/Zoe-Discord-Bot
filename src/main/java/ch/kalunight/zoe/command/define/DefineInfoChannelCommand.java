package ch.kalunight.zoe.command.define;

import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.service.InfoPanelRefresher;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

public class DefineInfoChannelCommand extends ZoeCommand {

  public DefineInfoChannelCommand() {
    this.name = "infochannel";
    this.arguments = "#mentionOfTheChannel";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "defineInfoChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DefineCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    Server server = ServerData.getServers().get(event.getGuild().getId());

    if(server.getInfoChannel() != null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "defineInfoChannelAlreadySet"), 
          server.getInfoChannel().getAsMention()));
    } else {
      if(event.getMessage().getMentionedChannels().size() != 1) {
        event.reply(LanguageManager.getText(server.getLangage(), "defineInfoChannelMentionOfAChannelNeeded"));
      } else {
        TextChannel textChannel = event.getMessage().getMentionedChannels().get(0);

        if(!textChannel.getGuild().equals(server.getGuild())) {
          event.reply(LanguageManager.getText(server.getLangage(), "defineInfoChannelMentionOfAChannel"));

        } else {
          if(!event.getMessage().getMentionedChannels().get(0).canTalk()) {
            event.reply(LanguageManager.getText(server.getLangage(), "defineInfoChannelMissingSpeakPermission"));
          } else {
            if(textChannel.equals(server.getConfig().getCleanChannelOption().getCleanChannel())) {
              event.reply(LanguageManager.getText(server.getLangage(), "defineInfoChannelImpossibleToDefineCleanChannel"));
            }else {
              server.setInfoChannel(textChannel.getIdLong());
              server.setControlePannel(new ControlPannel());
              event.reply(LanguageManager.getText(server.getLangage(), "defineInfoChannelDoneMessage"));

              if(server.getControlePannel().getInfoPanel().isEmpty()) {
                server.getControlePannel().getInfoPanel()
                .add(server.getInfoChannel().sendMessage(LanguageManager.getText(server.getLangage(), "defineInfoChannelLoadingMessage"))
                    .complete());
              }
              InfoPanelRefresher infoPanelRefresher = new InfoPanelRefresher(server);
              ServerData.getServerExecutor().submit(infoPanelRefresher);
            }
          }
        }
      }
    }
  }
}
