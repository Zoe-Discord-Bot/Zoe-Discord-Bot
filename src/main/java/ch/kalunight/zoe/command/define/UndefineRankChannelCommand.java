package ch.kalunight.zoe.command.define;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.RankHistoryChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class UndefineRankChannelCommand extends ZoeCommand {

  public UndefineRankChannelCommand() {
    this.name = "rankChannel";
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "undefineRankChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(UndefineCommand.USAGE_NAME, name, arguments, help);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    DTO.RankHistoryChannel rankChannel = RankHistoryChannelRepository.getRankHistoryChannel(server.serv_guildId);

    if(rankChannel == null) {
      event.reply(LanguageManager.getText(server.serv_language, "undefineRankChannelMissingChannel"));
    } else {
      RankHistoryChannelRepository.deleteRankHistoryChannel(rankChannel.rhChannel_id);
      event.reply(LanguageManager.getText(server.serv_language, "undefineRankChannelDoneMessage"));
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
