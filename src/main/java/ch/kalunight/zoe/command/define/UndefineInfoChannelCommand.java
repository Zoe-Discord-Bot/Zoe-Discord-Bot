package ch.kalunight.zoe.command.define;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.static_data.SpellingLangage;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class UndefineInfoChannelCommand extends Command {

  public UndefineInfoChannelCommand() {
    this.name = "infochannel";
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "undefineInfoChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(UndefineCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    Server server = ServerData.getServers().get(event.getGuild().getId());

    if(server == null) {
      server = new Server(event.getGuild(), SpellingLangage.EN, new ServerConfiguration());
      ServerData.getServers().put(event.getGuild().getId(), server);
    }

    if(server.getInfoChannel() == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "undefineInfoChannelMissingChannel"));
    } else {
      for(InfoCard infoCard : server.getControlePannel().getInfoCards()) {
        infoCard.getMessage().delete().queue();
        infoCard.getTitle().delete().queue();
      }
      for(Message message : server.getControlePannel().getInfoPanel()) {
        message.delete().queue();
      }
      server.setInfoChannel(null);
      server.setControlePannel(new ControlPannel());
      event.reply(LanguageManager.getText(server.getLangage(), "undefineInfoChannelDoneMessage"));
    }
  }
}
