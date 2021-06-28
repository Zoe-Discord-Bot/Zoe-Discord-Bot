package ch.kalunight.zoe.command.delete;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.delete.definition.DeletePlayerCommandClassicDefinition;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.RankHistoryChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class DeleteRankHistoryChannelCommand extends ZoeCommand {

  public DeleteRankHistoryChannelCommand() {
    this.name = "rankChannel";
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.help = "deleteRankChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeletePlayerCommandClassicDefinition.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {

    DTO.Server server = getServer(event.getGuild().getIdLong());

    DTO.RankHistoryChannel rankChannel = RankHistoryChannelRepository.getRankHistoryChannel(server.serv_guildId);

    if(rankChannel == null) {
      event.reply(LanguageManager.getText(server.getLanguage(), "deleteRankChannelNotSetted"));
    } else {
      try {
        TextChannel textChannel = event.getGuild().getTextChannelById(rankChannel.rhChannel_channelId);
        if(textChannel != null) {
          textChannel.delete().queue();
        }
      } catch(InsufficientPermissionException e) {
        RankHistoryChannelRepository.deleteRankHistoryChannel(rankChannel.rhChannel_id);
        event.reply(LanguageManager.getText(server.getLanguage(), "deleteRankChannelDeletedMissingPermission"));
        return;
      }

      RankHistoryChannelRepository.deleteRankHistoryChannel(rankChannel.rhChannel_id);
      event.reply(LanguageManager.getText(server.getLanguage(), "deleteRankChannelDoneMessage"));
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
