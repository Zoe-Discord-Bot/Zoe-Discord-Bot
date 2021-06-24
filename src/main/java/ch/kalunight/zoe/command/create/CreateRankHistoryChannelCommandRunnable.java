package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.RankHistoryChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class CreateRankHistoryChannelCommandRunnable {

  private CreateRankHistoryChannelCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(Server server, String nameChannel, Guild guild) throws SQLException {

    if(nameChannel == null || nameChannel.equals("")) {
      return LanguageManager.getText(server.getLanguage(), "nameOfInfochannelNeeded");
    }

    if(nameChannel.length() > 100) {
      return LanguageManager.getText(server.getLanguage(), "nameOfTheInfoChannelNeedToBeLess100Characters");
    }

    DTO.RankHistoryChannel rankChannelDb = RankHistoryChannelRepository.getRankHistoryChannel(guild.getIdLong());

    if(rankChannelDb != null && rankChannelDb.rhChannel_channelId != 0) {
      TextChannel rankChannel = guild.getTextChannelById(rankChannelDb.rhChannel_channelId);
      if(rankChannel == null) {
        RankHistoryChannelRepository.deleteRankHistoryChannel(rankChannelDb.rhChannel_id);
      }else {
        return String.format(LanguageManager.getText(server.getLanguage(), "rankChannelAlreadyExist"), rankChannel.getAsMention());
      }
    }

    TextChannel rankChannel = guild.createTextChannel(nameChannel).complete();

    ServerConfiguration serverConfiguration = ConfigRepository.getServerConfiguration(guild.getIdLong(), guild.getJDA());

    if(serverConfiguration.getZoeRoleOption().getRole() != null) {
      CommandUtil.giveRolePermission(guild, rankChannel, serverConfiguration);
    }
    RankHistoryChannelRepository.createRankHistoryChannel(server.serv_id, rankChannel.getIdLong());

    return LanguageManager.getText(server.getLanguage(), "rankChannelCorrectlyCreated");
  }
}
