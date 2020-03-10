package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.service.InfoPanelRefresher;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class CreateInfoChannelCommand extends ZoeCommand {

  public CreateInfoChannelCommand() {
    this.name = "infoChannel";
    this.arguments = "nameOfTheNewChannel";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.help = "createInfoChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    event.getTextChannel().sendTyping().complete();

    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    String nameChannel = event.getArgs();

    if(nameChannel == null || nameChannel.equals("")) {
      event.reply(LanguageManager.getText(server.serv_language, "nameOfInfochannelNeeded"));
      return;
    }

    if(nameChannel.length() > 100) {
      event.reply(LanguageManager.getText(server.serv_language, "nameOfTheInfoChannelNeedToBeLess100Characters"));
      return;
    }

    DTO.InfoChannel dbInfochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);

    if(dbInfochannel != null && dbInfochannel.infochannel_channelid != 0) {

      TextChannel infoChannel = event.getGuild().getTextChannelById(dbInfochannel.infochannel_channelid);
      if(infoChannel == null) {
        InfoChannelRepository.deleteInfoChannel(server);
      }else {
        event.reply(String.format(LanguageManager.getText(server.serv_language, "infochannelAlreadyExist"), infoChannel.getAsMention()));
        return;
      }
    }

    try {
      TextChannel infoChannel = event.getGuild().createTextChannel(nameChannel).complete();
      InfoChannelRepository.createInfoChannel(server.serv_id, infoChannel.getIdLong());
      Message message = infoChannel.sendMessage(LanguageManager.getText(server.serv_language, "infoChannelTitle")
            + "\n \n" + LanguageManager.getText(server.serv_language, "loading")).complete();

      dbInfochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);
      InfoChannelRepository.createInfoPanelMessage(dbInfochannel.infoChannel_id, message.getIdLong());

      ServerConfiguration config = ConfigRepository.getServerConfiguration(event.getGuild().getIdLong());
      
      if(config.getZoeRoleOption().getRole() != null) {
        CommandUtil.giveRolePermission(event.getGuild(), infoChannel, config);
      }
      
      event.reply(LanguageManager.getText(server.serv_language, "channelCreatedMessage"));
      
      if(!ServerData.isServerWillBeTreated(server)) {
        ServerData.getServersIsInTreatment().put(event.getGuild().getId(), true);
        ServerRepository.updateTimeStamp(server.serv_guildId, LocalDateTime.now());
        ServerData.getServerExecutor().execute(new InfoPanelRefresher(server, false));
      }
    } catch(InsufficientPermissionException e) {
      event.reply(LanguageManager.getText(server.serv_language, "impossibleToCreateInfoChannelMissingPerms"));
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
