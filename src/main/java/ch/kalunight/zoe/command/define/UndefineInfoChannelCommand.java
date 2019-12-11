package ch.kalunight.zoe.command.define;

import java.util.List;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class UndefineInfoChannelCommand extends ZoeCommand {

  public UndefineInfoChannelCommand() {
    this.name = "infochannel";
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "undefineInfoChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(UndefineCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    DTO.InfoChannel infochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);

    if(infochannel == null) {
      event.reply(LanguageManager.getText(server.serv_language, "undefineInfoChannelMissingChannel"));
    } else {
      
      for(InfoCard infoCard : server.getControlePannel().getInfoCards()) {
        infoCard.getMessage().delete().queue();
        infoCard.getTitle().delete().queue();
      }
      
      List<DTO.InfoPanelMessage> infoPanels = InfoChannelRepository.getInfoPanelMessages(server.serv_guildId);
      for(DTO.InfoPanelMessage message : infoPanels) {
        TextChannel textChannel = event.getGuild().getTextChannelById(infochannel.infochannel_channelid);
        textChannel.retrieveMessageById(message.infopanel_messageId).complete().delete().complete();
      }
      
      InfoChannelRepository.deleteInfoChannel(server.serv_guildId);
      event.reply(LanguageManager.getText(server.serv_language, "undefineInfoChannelDoneMessage"));
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
