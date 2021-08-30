package ch.kalunight.zoe.command.clash;

import java.sql.SQLException;
import java.util.List;
import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.service.clashchannel.TreatClashChannel;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class ClashRefreshCommandRunnable {
  
  public static final String USAGE_NAME = "refresh";
  
  private ClashRefreshCommandRunnable() {
    // hide default public constructor
  }
  
  public static void executeCommand(Server server, TextChannel channel, InteractionHook hook) throws SQLException {
    
    ClashChannel channelToRefresh = null;
    List<ClashChannel> clashchannls = ClashChannelRepository.getClashChannels(server.serv_guildId);
    for(ClashChannel channelToCheck : clashchannls) {
      if(channelToCheck.clashChannel_channelId == channel.getIdLong()) {
        channelToRefresh = channelToCheck;
        break;
      }
    }
    
    if(channelToRefresh != null) {
      String message = LanguageManager.getText(server.getLanguage(), "clashRefreshStarted"); //Complete here, avoid bad cleaning in clashChannel
      
      if(hook == null) {
        channel.sendMessage(message).complete();
      }else {
        hook.editOriginal(message).complete();
      }
      
      ServerThreadsManager.getClashChannelExecutor().execute(new TreatClashChannel(server, channelToRefresh, true));
    }else {
      String message = LanguageManager.getText(server.getLanguage(), "clashRefreshNeedToBeSendedInClashChannel");
      
      if(hook == null) {
        channel.sendMessage(message).queue();
      }else {
        hook.editOriginal(message).queue();
      }
    }
  }

}
