package ch.kalunight.zoe.command.delete;

import java.sql.SQLException;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class DeleteInfoChannelCommandRunnable {
  
  private DeleteInfoChannelCommandRunnable() {
    // hide default private constructor
  }

  public static String executeCommand(Server server, Guild guild) throws SQLException {

    DTO.InfoChannel infochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);

    if(infochannel == null) {
      return LanguageManager.getText(server.getLanguage(), "deleteInfoChannelChannelNotSetted");
    } else {
      try {
        TextChannel textChannel = guild.getTextChannelById(infochannel.infochannel_channelid);
        if(textChannel != null) {
          textChannel.delete().queue();
        }
      } catch(InsufficientPermissionException e) {
        InfoChannelRepository.deleteInfoChannel(server);
        return LanguageManager.getText(server.getLanguage(), "deleteInfoChannelDeletedMissingPermission");
      }

      InfoChannelRepository.deleteInfoChannel(server);
      return LanguageManager.getText(server.getLanguage(), "deleteInfoChannelDoneMessage");
    }
  }
}
