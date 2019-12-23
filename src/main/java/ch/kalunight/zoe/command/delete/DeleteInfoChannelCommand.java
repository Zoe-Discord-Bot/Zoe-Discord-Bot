package ch.kalunight.zoe.command.delete;

import java.sql.SQLException;
import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class DeleteInfoChannelCommand extends ZoeCommand {

  public DeleteInfoChannelCommand() {
    this.name = "infoChannel";
    this.arguments = "";
    this.help = "deleteInfoChannelHelpMessage";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeleteCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    event.getTextChannel().sendTyping().complete();
    
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    DTO.InfoChannel infochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);

    if(infochannel == null) {
      event.reply(LanguageManager.getText(server.serv_language, "deleteInfoChannelChannelNotSetted"));
    } else {
      try {
        TextChannel textChannel = event.getGuild().getTextChannelById(infochannel.infochannel_channelid);
        textChannel.delete().queue();
      } catch(InsufficientPermissionException e) {
        InfoChannelRepository.deleteInfoChannel(server);
        event.reply(LanguageManager.getText(server.serv_language, "deleteInfoChannelDeletedMissingPermission"));
        return;
      }

      InfoChannelRepository.deleteInfoChannel(server);
      event.reply(LanguageManager.getText(server.serv_language, "deleteInfoChannelDoneMessage"));
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
