package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import java.time.LocalDateTime;
import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.service.ServerChecker;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class CreateInfoChannelCommandRunnable {

  private CreateInfoChannelCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(Server server, String nameChannel, Guild guild) throws SQLException {
    
    if(nameChannel == null || nameChannel.equals("")) {
      return LanguageManager.getText(server.getLanguage(), "nameOfInfochannelNeeded");
    }

    if(nameChannel.length() > 100) {
      return LanguageManager.getText(server.getLanguage(), "nameOfTheInfoChannelNeedToBeLess100Characters");
    }

    DTO.InfoChannel dbInfochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);

    if(dbInfochannel != null && dbInfochannel.infochannel_channelid != 0) {

      TextChannel infoChannel = guild.getTextChannelById(dbInfochannel.infochannel_channelid);
      if(infoChannel == null) {
        InfoChannelRepository.deleteInfoChannel(server);
      }else {
        return String.format(LanguageManager.getText(server.getLanguage(), "infochannelAlreadyExist"), infoChannel.getAsMention());
      }
    }

    try {
      TextChannel infoChannel = guild.createTextChannel(nameChannel).complete();
      InfoChannelRepository.createInfoChannel(server.serv_id, infoChannel.getIdLong());
      Message message = infoChannel.sendMessage(LanguageManager.getText(server.getLanguage(), "infoChannelTitle")
            + "\n \n" + LanguageManager.getText(server.getLanguage(), "loading")).complete();

      dbInfochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);
      InfoChannelRepository.createInfoPanelMessage(dbInfochannel.infoChannel_id, message.getIdLong());

      ServerConfiguration config = ConfigRepository.getServerConfiguration(guild.getIdLong(), guild.getJDA());
      
      if(config.getZoeRoleOption().getRole() != null) {
        CommandUtil.giveRolePermission(guild, infoChannel, config);
      }
      
      if(!ServerThreadsManager.isServerWillBeTreated(server)) {
        ServerThreadsManager.getServersIsInTreatment().put(guild.getId(), true);
        ServerRepository.updateTimeStamp(server.serv_guildId, LocalDateTime.now());
        ServerChecker.getServerRefreshService().getServersAskedToRefresh().add(server);
      }
      
      return LanguageManager.getText(server.getLanguage(), "channelCreatedMessage");
    } catch(InsufficientPermissionException e) {
      return LanguageManager.getText(server.getLanguage(), "impossibleToCreateInfoChannelMissingPerms");
    }
  }
}
