package ch.kalunight.zoe.command.clash;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerThreadsManager;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.show.ShowCommand;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.service.clashchannel.TreatClashChannel;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;

public class ClashRefreshCommand extends ZoeCommand {
  
  public static final String USAGE_NAME = "refresh";
  
  public ClashRefreshCommand() {
    this.name = USAGE_NAME;
    String[] aliases = {"r"};
    this.arguments = "";
    this.aliases = aliases;
    this.help = "clashRefreshHelpMessage";
    this.cooldown = 10;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(ShowCommand.USAGE_NAME, name, arguments, help);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    ClashChannel channelToRefresh = null;
    List<ClashChannel> clashchannls = ClashChannelRepository.getClashChannels(server.serv_guildId);
    for(ClashChannel channelToCheck : clashchannls) {
      if(channelToCheck.clashChannel_channelId == event.getChannel().getIdLong()) {
        channelToRefresh = channelToCheck;
        break;
      }
    }
    
    if(channelToRefresh != null) {
      event.reply(LanguageManager.getText(server.getLanguage(), "clashRefreshStarted"));
      ServerThreadsManager.getClashChannelExecutor().execute(new TreatClashChannel(server, channelToRefresh, true));
    }else {
      event.reply(LanguageManager.getText(server.getLanguage(), "clashRefreshNeedToBeSendedInClashChannel"));
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
