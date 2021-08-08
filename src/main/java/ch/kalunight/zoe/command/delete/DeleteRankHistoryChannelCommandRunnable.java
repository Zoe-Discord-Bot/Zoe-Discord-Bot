package ch.kalunight.zoe.command.delete;

import java.sql.SQLException;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.RankHistoryChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class DeleteRankHistoryChannelCommandRunnable {

  private DeleteRankHistoryChannelCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(Server server, Guild guild) throws SQLException {

    DTO.RankHistoryChannel rankChannel = RankHistoryChannelRepository.getRankHistoryChannel(server.serv_guildId);

    if(rankChannel == null) {
      return LanguageManager.getText(server.getLanguage(), "deleteRankChannelNotSetted");
    } else {
      try {
        TextChannel textChannel = guild.getTextChannelById(rankChannel.rhChannel_channelId);
        if(textChannel != null) {
          textChannel.delete().queue();
        }
      } catch(InsufficientPermissionException e) {
        RankHistoryChannelRepository.deleteRankHistoryChannel(rankChannel.rhChannel_id);
        return LanguageManager.getText(server.getLanguage(), "deleteRankChannelDeletedMissingPermission");
      }

      RankHistoryChannelRepository.deleteRankHistoryChannel(rankChannel.rhChannel_id);
      return LanguageManager.getText(server.getLanguage(), "deleteRankChannelDoneMessage");
    }
  }

}
